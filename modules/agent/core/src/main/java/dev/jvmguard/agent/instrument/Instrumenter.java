package dev.jvmguard.agent.instrument;

import dev.jvmguard.agent.AgentProperties;
import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.instrument.bytecodeVisitors.CheckClassVisitor;
import dev.jvmguard.agent.instrument.bytecodeVisitors.InstrumentationClassVisitor;
import dev.jvmguard.agent.instrument.bytecodeVisitors.LoadClassInstrVisitor;
import dev.jvmguard.agent.instrument.bytecodeVisitors.SystemInstrVisitor;
import dev.jvmguard.agent.instrument.classInfo.ClassFileInfo;
import dev.jvmguard.agent.instrument.classInfo.ClassFileInfo.HierarchyVisitor;
import dev.jvmguard.agent.instrument.classInfo.ClassFileInfo.PartiallyDefinedInfo;
import dev.jvmguard.agent.instrument.classInfo.DeclaredAnnotations;
import dev.jvmguard.agent.instrument.interceptions.BaseInterception;
import dev.jvmguard.agent.instrument.interceptions.TransactionInterception;
import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.instrument.transaction.DefinitionWithHandler;
import dev.jvmguard.agent.instrument.transaction.TransactionDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationChangeCalculator;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationTransactionDefList;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedChangeCalculator;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedDefinition;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedTransactionDefList;
import dev.jvmguard.agent.telemetry.TelemetryCollector;
import dev.jvmguard.agent.util.Logger;
import dev.jvmguard.agent.util.LoggingHandler;
import dev.jvmguard.agent.util.JvmGuardUtil;
import dev.jvmguard.agent.util.collection.WeakIdentityHashMap;
import dev.jvmguard.agent.util.reflection.ReflectionUtil;
import dev.jvmguard.annotation.Telemetry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class Instrumenter {
    public static final int HARD_FILTERED_SYSTEM = 2;
    public static final int HARD_FILTERED_FILTERED = 3;
    public static final int HARD_FILTERED_UNFILTERED = 4;

    public static final boolean SAVE_INSTRUMENTED = AgentProperties.getBoolean("saveInstrumented");
    public static final boolean SAVE_HIERARCHICAL = AgentProperties.getBoolean("saveHierarchical");
    public static final boolean SAVE_ALL = AgentProperties.getBoolean("saveAllClasses");

    private static final Object PRESENT = new Object();
    private static final String CLASS_EXTENSION = ".class";

    private static final String[] HARD_FILTERED_PACKAGES;

    static {
        String[] hardFiltered = {
            "java/",
            "jdk/",
            "sun/",
            "__jvmguard",
            "dev/jvmguard/agent/",
            "dev/jvmguard/annotation/",
            "org/mozilla/javascript/gen/"
        };
        try {
            String additionalPackages = AgentProperties.getProperty("filteredPackages");
            if (additionalPackages != null) {
                String[] additionalPackageArray = additionalPackages.split(";");
                String[] all = new String[hardFiltered.length + additionalPackageArray.length];
                System.arraycopy(hardFiltered, 0, all, 0, hardFiltered.length);
                System.arraycopy(additionalPackageArray, 0, all, hardFiltered.length, additionalPackageArray.length);
                hardFiltered = all;
            }
        } catch (Throwable ignored) {
        }
        HARD_FILTERED_PACKAGES = hardFiltered;
    }

    private final Map<String, ClassFileInfo> classFileInfos = Collections.synchronizedMap(new HashMap<>());

    private final InstrumenterConfig instrumenterConfig = new InstrumenterConfig();

    private final Map<String, PartiallyDefinedInfo> partiallyDefined = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Map<InterceptionMethod, Telemetry>> telemetryMethods = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class, Object> classesWithAddedClassInit = Collections.synchronizedMap(new WeakIdentityHashMap<>());
    private final Set<String> classNamesWithNativeInstr = Collections.synchronizedSet(new HashSet<>());

    @GuardedBy("calleeMap")
    private final Map<TransactionDefinition, Map<String, Class>> calleeMap = new HashMap<>();
    @GuardedBy("calleeMap")
    private final Map<DefinitionWithHandler, Class> usedCallees = new HashMap<>();

    private final Map<String, DeclaredAnnotations> declaredAnnotationMap = Collections.synchronizedMap(new HashMap<>());

    @GuardedBy("classesToStoredMethodAnnotations")
    private final Map<String, Map<String, Set<InterceptionMethod>>> classesToStoredMethodAnnotations = new HashMap<>();

    private final SystemInstrVisitor systemInstrVisitor = new SystemInstrVisitor();

    public List<Class> calculateChanges(Map<MatchedDefinition, MatchedTransactionDefList> pojoInterceptionMap,
                                        Map<AnnotationDefinition, AnnotationTransactionDefList> annotationInterceptionMap,
                                        Set<AnnotationDefinition> oldAnnotations,
                                        Set<MatchedDefinition> oldMatchedDefinitions,
                                        Instrumentation instrumentation) {
        List<Class> retransformClasses = new ArrayList<>();
        MatchedChangeCalculator pojoChangeCalculator;
        AnnotationChangeCalculator annotationChangeCalculator;

        Logger.log(Subsystem.INSTRUMENTATION, 3, true, "pojo defs %s\n", pojoInterceptionMap);
        Logger.log(Subsystem.INSTRUMENTATION, 3, true, "annotation defs %s\n", annotationInterceptionMap);
        synchronized (calleeMap) {
            Logger.log(Subsystem.INSTRUMENTATION, 3, true, "old callees %s\n", calleeMap);

            pojoChangeCalculator = new MatchedChangeCalculator(pojoInterceptionMap, oldMatchedDefinitions, this, calleeMap, usedCallees);
            annotationChangeCalculator = new AnnotationChangeCalculator(annotationInterceptionMap, oldAnnotations, this, calleeMap, usedCallees);

            clearCallees();
            pojoChangeCalculator.apply();
            annotationChangeCalculator.apply();
        }

        for (Class clazz : instrumentation.getAllLoadedClasses()) {
            pojoChangeCalculator.visitClass(clazz, clazz.getName());
            annotationChangeCalculator.visitClass(clazz, clazz.getName());

            String className = clazz.getName();
            if ((pojoChangeCalculator.shouldRetransform(className) || annotationChangeCalculator.shouldRetransform(className)) && !clazz.isInterface() && instrumentation.isModifiableClass(clazz)) {
                retransformClasses.add(clazz);
            }
        }
        synchronized (calleeMap) {
            Logger.log(Subsystem.INSTRUMENTATION, 3, true, "new callees %s\n", calleeMap);
        }
        return retransformClasses;
    }

    public Class getTargetClass(String className, TransactionInterception transactionInterception) throws ClassNotFoundException {
        synchronized (calleeMap) {
            DefinitionWithHandler definitionWithHandler = new DefinitionWithHandler(transactionInterception.getDefinition(), transactionInterception.getHandler());
            Class targetClass = usedCallees.get(definitionWithHandler);
            if (targetClass == null) {
                targetClass = TargetClassGenerator.getInstance().generate(transactionInterception);
                usedCallees.put(definitionWithHandler, targetClass);
            }

            Map<String, Class> classNameToTarget = calleeMap.computeIfAbsent(transactionInterception.getDefinition(), k -> new HashMap<>());
            classNameToTarget.put(className.replace('/', '.'), targetClass);
            return targetClass;
        }
    }


    @SuppressWarnings("UnusedParameters")
    public void initFeatures() {
        if (SAVE_INSTRUMENTED || SAVE_ALL) {
            JvmGuardUtil.emptyDirectory(getDebugClassFileDir(), Collections.emptySet());
        }
    }

    public void registerClassLoad(Instrumentation instrumentation, boolean addedClinit, Class clazz) {
        checkTelemetryMethods(clazz);

        final PartiallyDefinedInfo partiallyDefinedInfo = partiallyDefined.remove(clazz.getName());
        if (partiallyDefinedInfo != null) {
            ClassFileInfo classFileInfo = partiallyDefinedInfo.getClassFileInfo();
            if (addedClinit) {
                classesWithAddedClassInit.put(clazz, PRESENT);
            }
            boolean retransform = !classFileInfo.visit(new HierarchyVisitor() {
                @Override
                public boolean visit(ClassFileInfo classFileInfo) {
                    if (partiallyDefinedInfo.getUnsureSuperclasses().contains(classFileInfo.getName())) {
                        return false;
                    }
                    return true;
                }
            });
            if (Logger.isEnabled(Subsystem.INSTRUMENTATION, 10)) {
                Logger.log(Subsystem.INSTRUMENTATION, 10, false, "registered class (1) " + classFileInfo.getName() + ", " + partiallyDefined + "; " + retransform);
            }
            if (!retransform) {
                Set<BaseInterception> classInterceptions = new HashSet<>();

                DeclaredAnnotations declaredAnnotations = getDeclaredAnnotations(clazz.getName());
                InterceptionClassHierarchyVisitor interceptionClassHierarchyVisitor = new InterceptionClassHierarchyVisitor(getPojoClassDefinitions(), getAnnotationDefinitions(), classInterceptions, declaredAnnotations, this);
                classFileInfo.visit(interceptionClassHierarchyVisitor);
                retransform = classInterceptions.size() != partiallyDefinedInfo.getClassInterceptionCount();
                if (Logger.isEnabled(Subsystem.INSTRUMENTATION, 10)) {
                    Logger.log(Subsystem.INSTRUMENTATION, 10, false, "registered class (2) " + classInterceptions.size() + ", " + partiallyDefinedInfo.getClassInterceptionCount());
                }
            }
            if (retransform) {
                Logger.log(Subsystem.INSTRUMENTATION, 2, false, "retransforming class %s\n", clazz.getName());
                try {
                    if (instrumentation.isModifiableClass(clazz)) {
                        RetransformationUtil.doRetransformation(instrumentation, clazz);
                    }
                } catch (Throwable e) {
                    JvmGuardAgent.log(e);
                }
            }
        }
        //MessageUtil.debugOut("NOT FOUND " + clazz.getName() + ", " + partiallyDefined);
    }

    private void checkTelemetryMethods(Class clazz) {
        Map<InterceptionMethod, Telemetry> methods = telemetryMethods.remove(clazz.getName());
        if (methods != null) {
            try {
                for (final Method method : clazz.getDeclaredMethods()) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        InterceptionMethod declaredInterceptionMethod = new InterceptionMethod(method.getName(), Type.getMethodDescriptor(method));
                        Telemetry telemetry = methods.get(declaredInterceptionMethod);
                        if (telemetry != null) {
                            TelemetryCollector.getInstance().addAnnotationTelemetry(ReflectionUtil.setAccessible(method), telemetry);
                            Logger.log(Subsystem.INSTRUMENTATION, 5, true, "added custom telemetry for %s\n", method);
                        }
                    }
                }
            } catch (Throwable t) {
                Logger.log(Subsystem.INSTRUMENTATION, 0, true, "error accessing custom telemetry of %s\n", clazz);
                Logger.log(Subsystem.INSTRUMENTATION, 0, true, t);
            }
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined, byte[] classFileBuffer) {
        int hardFilterState = isHardFiltered(className);
        Logger.log(Subsystem.INSTRUMENTATION, 10, true, "loading class %s, %s %s\n", className, hardFilterState, classBeingRedefined);
        if (hardFilterState == HARD_FILTERED_FILTERED) {
            return null;
        }
        byte[] originalClassFileBuffer = classFileBuffer;

        ClassReader classReader = new ClassReader(classFileBuffer);

        boolean instrumented = false;
        if (systemInstrVisitor.isInstrumented(className)) {
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
            classReader.accept(new SystemInstrVisitor(classWriter, checkNativeInstrumentation(className, classBeingRedefined), systemInstrVisitor), ClassReader.EXPAND_FRAMES);
            classFileBuffer = classWriter.toByteArray();
            classReader = new ClassReader(classFileBuffer);
            instrumented = true;
        }

        if (hardFilterState == HARD_FILTERED_UNFILTERED) {
            CheckClassVisitor checkClassVisitor = new CheckClassVisitor(this, classBeingRedefined != null, loader != null);
            classReader.accept(checkClassVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (checkClassVisitor.isLoadClass()) {
                ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                classReader.accept(new LoadClassInstrVisitor(classWriter, className), ClassReader.EXPAND_FRAMES);
                classFileBuffer = classWriter.toByteArray();
                classReader = new ClassReader(classFileBuffer);
                instrumented = true;
            }
            if (checkClassVisitor.getTelemetryMethods() != null) {
                telemetryMethods.put(className.replace('/', '.'), checkClassVisitor.getTelemetryMethods());
            }

            PartiallyDefinedInfo partiallyDefinedInfo = checkClassVisitor.getPartiallyDefinedInfo(false);
            if (partiallyDefinedInfo != null) {
                if (shouldCheckRetransform(loader, className)) {
                    partiallyDefined.put(className.replace('/', '.'), partiallyDefinedInfo);
                    Logger.log(Subsystem.INSTRUMENTATION, 10, false, "class %s is still partially defined: %s\n", className, partiallyDefinedInfo);
                } else {
                    partiallyDefinedInfo = null;
                }
            }

            Object clInitAdded = classesWithAddedClassInit.get(classBeingRedefined);
            boolean registerClass = (loader == null && partiallyDefinedInfo != null) || clInitAdded != null;
            if (registerClass || !checkClassVisitor.getClassInterceptions().isEmpty() || !checkClassVisitor.getMethodInterceptions().isEmpty()) {
                ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                InstrumentationClassVisitor classVisitor = new InstrumentationClassVisitor(classWriter, className, this,
                        registerClass, classBeingRedefined == null || clInitAdded != null).
                    classInterceptions(checkClassVisitor.getClassInterceptions()).
                    methodInterceptions(checkClassVisitor.getMethodInterceptions());

                classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                if (classVisitor.isInstrumented()) {
                    Logger.log(Subsystem.INSTRUMENTATION, 1, true, "instrumenting %s (%d, %d)\n", className, checkClassVisitor.getClassInterceptions().size(), checkClassVisitor.getMethodInterceptions().size());
                    classFileBuffer = classWriter.toByteArray();
                    instrumented = true;
                }
            }
        }
        if (instrumented) {
            if (SAVE_INSTRUMENTED) {
                String suffix = saveClassFile(className, classFileBuffer);
                saveClassFile(className + suffix + ".orig", originalClassFileBuffer);
            }
            return classFileBuffer;
        } else if (SAVE_ALL) {
            saveClassFile(className + ".orig", originalClassFileBuffer);
        }
        return null;
    }


    private boolean checkNativeInstrumentation(String className, Class classBeingRedefined) {
        if (classBeingRedefined == null) {
            classNamesWithNativeInstr.add(className);
            return true;
        } else {
            return classNamesWithNativeInstr.contains(className);
        }
    }

    private boolean shouldCheckRetransform(ClassLoader loader, String className) {
        if (className.startsWith("jdk/internal/reflect/")) {
            return false;
        }
        if (loader == null) {
            return !className.startsWith("javax/") && !className.startsWith("com/sun/");
        }
        return true;
    }

    public static String saveClassFile(String className, byte[] ret) {
        String suffix = "";
        try {
            File dir = getDebugClassFileDir();
            File file = new File(dir, (SAVE_HIERARCHICAL ? className : className.replace('/', '.')) + CLASS_EXTENSION);
            file.getParentFile().mkdirs();
            String baseFileName = file.getName().substring(0, file.getName().length() - CLASS_EXTENSION.length());
            int index = 1;
            while (file.exists()) {
                suffix = "_" + ++index;
                file = new File(file.getParentFile(), baseFileName + suffix + CLASS_EXTENSION);
            }

            RandomAccessFile raFile = new RandomAccessFile(file, "rw");
            raFile.setLength(0);
            raFile.write(ret);
            raFile.close();
        } catch (IOException e) {
            Logger.log(Subsystem.COMMON, 0, true, e);
        }
        return suffix;
    }

    private static File getDebugClassFileDir() {
        return new File(LoggingHandler.getLogFile().getAbsolutePath() + ".classes");
    }

    private boolean isHardFilteredPackage(String name) {
        for (String filteredPackage : HARD_FILTERED_PACKAGES) {
            if (name.startsWith(filteredPackage)) {
                return true;
            }
        }
        return false;
    }

    public int isHardFiltered(String name) {
        if (isHardFilteredPackage(name)) {
            if (systemInstrVisitor.isInstrumented(name)) {
                return HARD_FILTERED_SYSTEM;
            } else {
                return HARD_FILTERED_FILTERED;
            }
        } else {
            return HARD_FILTERED_UNFILTERED;
        }
    }


    public Map<InterceptionMethod, List<MatchedTransactionDefList>> getPojoMethodDefinitions() {
        return instrumenterConfig.getPojoMethodDefinitions();
    }

    public Map<String, List<MatchedTransactionDefList>> getPojoClassDefinitions() {
        return instrumenterConfig.getPojoClassDefinitions();
    }

    public Map<String, List<AnnotationTransactionDefList>> getAnnotationDefinitions() {
        return instrumenterConfig.getAnnotationDefinitions();
    }

    public Set<String> getInheritableMethodAnnotations() {
        return instrumenterConfig.getInheritableMethodAnnotations();
    }

    public void registerLoadedClass(String className, String superClassName, String[] declaredInterfaceNames, boolean transactionInstrumentable) {
        registerClass(className, superClassName, declaredInterfaceNames, transactionInstrumentable);
    }

    public ClassFileInfo registerClass(String className, String superClassName, String[] declaredInterfaceNames, boolean transactionInstrumentable) {
        ClassFileInfo classFileInfo = getClassFileInfo(className);
        classFileInfo.setSuperClass(getClassFileInfo(superClassName));
        if (declaredInterfaceNames != null && declaredInterfaceNames.length > 0) {
            ClassFileInfo[] declaredInterfaces = new ClassFileInfo[declaredInterfaceNames.length];
            for (int i = 0; i < declaredInterfaceNames.length; i++) {
                declaredInterfaces[i] = getClassFileInfo(declaredInterfaceNames[i]);
            }
            classFileInfo.setInterfaces(declaredInterfaces);
        }
        if (transactionInstrumentable) {
            classFileInfo.setTransactionInstrumentable();
        }
        classFileInfo.setDefined();
        return classFileInfo;
    }

    public ClassFileInfo getClassFileInfo(String className) {
        if (className == null) {
            return null;
        }
        ClassFileInfo classFileInfo = classFileInfos.get(className);
        if (classFileInfo == null) {
            classFileInfo = new ClassFileInfo(className, isHardFiltered(className) != HARD_FILTERED_UNFILTERED);
            ClassFileInfo previous = classFileInfos.putIfAbsent(className, classFileInfo);
            if (previous != null) {
                return previous;
            }
        }
        return classFileInfo;
    }


    public void visitClassFileInfos(HierarchyVisitor visitor) {
        for (Object o : classFileInfos.values().toArray()) {
            ClassFileInfo classFileInfo = (ClassFileInfo)o;
            if (!classFileInfo.isNoTransaction()) {
                classFileInfo.visit(visitor);
            }
        }
    }

    private void clearCallees() {
        synchronized (calleeMap) {
            usedCallees.clear();
            calleeMap.clear();
        }
    }


    public void setPojoMethodDefinitions(Map<InterceptionMethod, List<MatchedTransactionDefList>> pojoMethodDefinitions) {
        instrumenterConfig.setPojoMethodDefinitions(pojoMethodDefinitions);
    }

    public void setPojoClassDefinitions(Map<String, List<MatchedTransactionDefList>> pojoClassDefinitions) {
        instrumenterConfig.setPojoClassDefinitions(pojoClassDefinitions);
    }

    public void setAnnotationDefinitions(Map<String, List<AnnotationTransactionDefList>> annotationDefinitions, Set<String> inheritedMethodAnnotations) {
        instrumenterConfig.setAnnotationDefinitions(annotationDefinitions, inheritedMethodAnnotations);
    }

    public void freeMemory() {
        classFileInfos.clear();
        partiallyDefined.clear();
        clearCallees();
        classesWithAddedClassInit.clear();
        classNamesWithNativeInstr.clear();
        instrumenterConfig.freeMemory();
    }

    public Collection<Class> initLoadedClasses(Instrumentation instrumentation) {
        List<Class> retransformClasses = new ArrayList<>();
        for (Class klass : instrumentation.getAllLoadedClasses()) {
            String[] interfaceNames = null;
            Class[] interfaces = klass.getInterfaces();
            if (interfaces.length > 0) {
                interfaceNames = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    interfaceNames[i] = interfaces[i].getName();
                }
            }
            Class superclass = klass.getSuperclass();
            registerLoadedClass(klass.getName(), superclass == null ? null : superclass.getName(), interfaceNames, klass.getClassLoader() != null && isHardFiltered(klass.getName().replace('.', '/')) == HARD_FILTERED_UNFILTERED);
        }
        return retransformClasses;
    }

    public void setDeclaredAnnotations(String className, DeclaredAnnotations annotations) {
        if (annotations == null) {
            declaredAnnotationMap.remove(className);
        } else {
            declaredAnnotationMap.put(className, annotations);
        }
    }

    public DeclaredAnnotations getDeclaredAnnotations(String className) {
        return declaredAnnotationMap.get(className);
    }

    public Set<InterceptionMethod> getMethodAnnotations(String className, String annotationName) {
        synchronized (classesToStoredMethodAnnotations) {
            Map<String, Set<InterceptionMethod>> annotationsToMethods = classesToStoredMethodAnnotations.get(className);
            if (annotationsToMethods != null) {
                return annotationsToMethods.get(annotationName);
            }
        }
        return null;
    }

    public void setMethodAnnotations(String className, String annotationName, Set<InterceptionMethod> methods) {
        synchronized (classesToStoredMethodAnnotations) {
            Map<String, Set<InterceptionMethod>> annotationsToMethods = classesToStoredMethodAnnotations.computeIfAbsent(className, k -> new HashMap<>());
            annotationsToMethods.put(annotationName, methods);
        }
    }

}
