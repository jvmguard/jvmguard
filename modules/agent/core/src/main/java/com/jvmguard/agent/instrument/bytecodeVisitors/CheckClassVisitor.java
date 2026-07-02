package com.jvmguard.agent.instrument.bytecodeVisitors;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.callee.Handler;
import com.jvmguard.agent.callee.PojoHandler;
import com.jvmguard.agent.instrument.Instrumenter;
import com.jvmguard.agent.instrument.InterceptionClassHierarchyVisitor;
import com.jvmguard.agent.instrument.classInfo.ClassFileInfo;
import com.jvmguard.agent.instrument.classInfo.ClassFileInfo.PartiallyDefinedInfo;
import com.jvmguard.agent.instrument.classInfo.ClassFileInfo.TriState;
import com.jvmguard.agent.instrument.classInfo.DevOpsAnnotationInfo;
import com.jvmguard.agent.instrument.classInfo.DevOpsAnnotations;
import com.jvmguard.agent.instrument.interceptions.BaseInterception;
import com.jvmguard.agent.instrument.interceptions.DevOpsConcreteMethodInterception;
import com.jvmguard.agent.instrument.interceptions.TransactionInterception;
import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.agent.instrument.transaction.DefinitionSite;
import com.jvmguard.agent.instrument.transaction.DefinitionSite.AnnotationDefinitionSite;
import com.jvmguard.agent.instrument.transaction.TransactionDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.AnnotationTransactionDefList;
import com.jvmguard.agent.instrument.transaction.annotation.CustomAnnotationDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.DevOpsAnnotationDefinition;
import com.jvmguard.agent.instrument.transaction.pojo.PojoDefinition;
import com.jvmguard.agent.instrument.transaction.pojo.PojoTransactionDefList;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.annotation.ClassTransaction;
import com.jvmguard.annotation.Inheritance.Mode;
import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Telemetry;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.Map.Entry;

import static com.jvmguard.agent.AgentConstants.ASM_VERSION;
import static org.objectweb.asm.Opcodes.*;

public class CheckClassVisitor extends ClassVisitor {
    private Instrumenter instrumenter;

    private final InterceptionMethod lookupMethod = new InterceptionMethod(null, null);
    private final DefinitionSite lookupDefinitionSite = new DefinitionSite();
    private final AnnotationDefinitionSite lookupAnnotationDefinitionSite = new AnnotationDefinitionSite();

    private ClassFileInfo classFileInfo;
    private PartiallyDefinedInfo partiallyDefinedInfo;

    private final Set<BaseInterception> classInterceptions = new HashSet<>();
    private final Map<InterceptionMethod, Set<BaseInterception>> methodInterceptions = new HashMap<>();
    private final IntOpenHashSet exclusiveIdentifiers = new IntOpenHashSet();

    private Set<InterceptionMethod> publicMethods;
    private Set<TransactionDefinition> definitionsWithPublicMethods = Collections.newSetFromMap(new IdentityHashMap<>());

    private Set<Object> classAnnotations = new HashSet<>();
    private Set<Object> methodAnnotations = new HashSet<>();

    private Map<InterceptionMethod, Telemetry> telemetryMethods;

    private Map<String, Set<InterceptionMethod>> inheritableMethodAnnotationsToMethods = new HashMap<>();

    private boolean loadClass;

    private int access;
    private String slashClassName;
    private String dottedClassName;
    private final boolean redefined;
    private boolean transactionInstrumentation;

    private DevOpsAnnotationInfo devOpsClassTransactionInfo;

    private DevOpsAnnotations devOpsAnnotations;
    private DevOpsAnnotations previousDevOpsAnnotations;

    public CheckClassVisitor(Instrumenter instrumenter, boolean redefined, boolean transactionInstrumentation) {
        super(ASM_VERSION, null);
        this.instrumenter = instrumenter;
        this.redefined = redefined;
        this.transactionInstrumentation = transactionInstrumentation;
    }

    public boolean isInterface() {
        return (access & ACC_INTERFACE) > 0;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.access = access;
        this.slashClassName = name;
        this.dottedClassName = name.replace('/', '.');
        if (redefined) {
            previousDevOpsAnnotations = instrumenter.getDevOpsAnnotations(dottedClassName);
        }

        for (int i = 0; i < interfaces.length; i++) {
            interfaces[i] = interfaces[i].intern();
        }
        classFileInfo = instrumenter.registerClass(name.intern(), superName.intern(), interfaces, transactionInstrumentation);
        List<PojoTransactionDefList> transactionDefLists = instrumenter.getPojoClassDefinitions().get(classFileInfo.getName());
        if (transactionDefLists != null) {
            for (PojoTransactionDefList transactionDefList : transactionDefLists) {
                if (transactionDefList.getDefinition().isSuperclassWithImplementingMethods()) {
                    definitionsWithPublicMethods.add(transactionDefList.getDefinition());
                    if (publicMethods == null) {
                        publicMethods = new HashSet<>();
                    }
                }
            }
        }
    }

    private DevOpsAnnotations getDevOpsAnnotations() {
        if (devOpsAnnotations == null) {
            devOpsAnnotations = new DevOpsAnnotations();
        }
        return devOpsAnnotations;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
        if (DevOpsAnnotationDefinition.NO_TRANSACTION_DESCRIPTOR.equals(desc)) {
            getDevOpsAnnotations().setNoTransaction(true);
        } else if (DevOpsAnnotationDefinition.CLASS_TRANSACTION_DESCRIPTOR.equals(desc)) {
            if (!redefined) {
                return new JvmGuardAnnotationVisitor(ClassTransaction.class, classTransaction -> {
                    handleDevOpsClass(classTransaction);
                    getDevOpsAnnotations().setClassTransaction(classTransaction);
                });
            }
        } else if (visible) {
            classAnnotations.add(desc.intern());
            List<AnnotationTransactionDefList> annotationDefLists = instrumenter.getAnnotationDefinitions().get(desc);
            if (annotationDefLists != null) {
                for (AnnotationTransactionDefList annotationDefList : annotationDefLists) {
                    if (annotationDefList.getDefinition() instanceof CustomAnnotationDefinition) {
                        CustomAnnotationDefinition customAnnotationDefinition = (CustomAnnotationDefinition)annotationDefList.getDefinition();
                        if (customAnnotationDefinition.isClassWithImplementingOnly()) {
                            definitionsWithPublicMethods.add(customAnnotationDefinition);
                            if (publicMethods == null) {
                                publicMethods = new HashSet<>();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void handleDevOpsClass(ClassTransaction classTransaction) {
        if (classTransaction.inheritance().value() == Mode.NONE) {
            classAnnotations.add((DevOpsAnnotationDefinition.CLASS_TRANSACTION_DESCRIPTOR + classTransaction.group()).intern());
        } else {
            devOpsClassTransactionInfo = DevOpsAnnotationInfo.create(classTransaction, dottedClassName);
            classAnnotations.add(devOpsClassTransactionInfo);
            if (classTransaction.inheritance().implementingOnly() && publicMethods == null) {
                publicMethods = new HashSet<>();
            }
        }
    }

    private void addMethodInterception(InterceptionMethod method, BaseInterception interception) {
        if (interception.getExclusiveIdentifier() == 0 || exclusiveIdentifiers.add(interception.getExclusiveIdentifier())) {
            Set<BaseInterception> set = methodInterceptions.get(method);
            if (set == null) {
                set = new LinkedHashSet<>();
                methodInterceptions.put(new InterceptionMethod(method.getMethodName(), method.getMethodSignature()), set);
            }
            set.add(interception);
        }
    }

    public PartiallyDefinedInfo getPartiallyDefinedInfo(boolean create) {
        if (partiallyDefinedInfo == null && create) {
            partiallyDefinedInfo = classFileInfo.createPartiallyDefinedInfo();
        }
        return partiallyDefinedInfo;
    }

    public boolean isLoadClass() {
        return loadClass;
    }

    public Map<InterceptionMethod, Telemetry> getTelemetryMethods() {
        return telemetryMethods;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String methodDesc, String signature, String[] exceptions) {
        if (LoadClassInstrVisitor.isLoadClassMethod(slashClassName, access, name, methodDesc)) {
            loadClass = true;
        }
        if (publicMethods != null && (access & ACC_STATIC) == 0 && (access & ACC_PUBLIC) > 0 && !name.startsWith("<")) {
            InterceptionMethod interceptionMethod = new InterceptionMethod(name, methodDesc);
            publicMethods.add(interceptionMethod);
        }

        if (!isInterface()) {
            if (transactionInstrumentation) {
                checkPojoDefinitions(instrumenter.getPojoMethodDefinitions().get(lookupMethod.init(null, name, methodDesc)));
                checkPojoDefinitions(instrumenter.getPojoMethodDefinitions().get(lookupMethod.init(null, name, null)));
            }
        }
        return !transactionInstrumentation ? null : new MethodVisitor(ASM_VERSION) {
            @Override
            public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
                Logger.log(Subsystem.INSTRUMENTATION, 20, false, "found method annotation %s (%s) for %s\n", desc, visible, instrumenter.getAnnotationDefinitions());
                if (DevOpsAnnotationDefinition.TELEMETRY_DESCRIPTOR.equals(desc)) {
                    if (!redefined && (access & ACC_STATIC) > 0) {
                        if (Type.getArgumentTypes(methodDesc).length == 0) {
                            Type returnType = Type.getReturnType(methodDesc);
                            if (returnType.equals(Type.INT_TYPE) || returnType.equals(Type.LONG_TYPE) || returnType.equals(Type.SHORT_TYPE) || returnType.equals(Type.FLOAT_TYPE) || returnType.equals(Type.DOUBLE_TYPE) ||
                                returnType.equals(Type.getType(Integer.class)) || returnType.equals(Type.getType(Long.class)) || returnType.equals(Type.getType(Short.class)) || returnType.equals(Type.getType(Float.class)) || returnType.equals(Type.getType(Double.class))) {
                                return new JvmGuardAnnotationVisitor(Telemetry.class, annotation -> {
                                    Logger.log(Subsystem.INSTRUMENTATION, 1, false, "found custom telemetry %s.%s %s\n", dottedClassName, name, annotation);
                                    if (telemetryMethods == null) {
                                        telemetryMethods = new HashMap<>();
                                    }
                                    telemetryMethods.put(new InterceptionMethod(name, methodDesc), annotation);
                                });

                            }
                        }
                    }
                } else if (DevOpsAnnotationDefinition.NO_TRANSACTION_DESCRIPTOR.equals(desc)) {
                    if (!redefined) {
                        getDevOpsAnnotations().addNoTransactionMethod(new InterceptionMethod(name, methodDesc));
                    }
                } else if (DevOpsAnnotationDefinition.METHOD_TRANSACTION_DESCRIPTOR.equals(desc)) {
                    if (!redefined) {
                        return new JvmGuardAnnotationVisitor(MethodTransaction.class, methodTransaction -> {
                            InterceptionMethod interceptionMethod = new InterceptionMethod(name, methodDesc);
                            handleDevOpsMethod(interceptionMethod, methodTransaction);
                            getDevOpsAnnotations().addMethodTransaction(interceptionMethod, methodTransaction);
                        });
                    }
                } else if (visible) {
                    final List<AnnotationTransactionDefList> transactionDefLists = instrumenter.getAnnotationDefinitions().get(desc);
                    if (transactionDefLists != null) {
                        for (AnnotationTransactionDefList transactionDefList : transactionDefLists) {
                            AnnotationDefinition annotationDefinition = transactionDefList.getDefinition();
                            if (annotationDefinition.isMethodAnnotation() && !annotationDefinition.isInheritable()) { // inheritable method annotations will be handled by the class hierarchy visitor.
                                Handler handler = transactionDefList.getHandler(lookupAnnotationDefinitionSite.init(dottedClassName, dottedClassName));
                                if (handler != null) {
                                    addMethodInterception(lookupMethod, new TransactionInterception(annotationDefinition, handler));
                                }
                            }
                        }
                    }
                    String internedDesc = desc.intern();
                    methodAnnotations.add(internedDesc);
                    if (instrumenter.getInheritableMethodAnnotations().contains(internedDesc)) {
                        Set<InterceptionMethod> methods = inheritableMethodAnnotationsToMethods.computeIfAbsent(internedDesc, k -> new HashSet<>());
                        methods.add(new InterceptionMethod(name, methodDesc));
                    }
                }
                return null;
            }
        };
    }

    private void handleDevOpsMethod(InterceptionMethod interceptionMethod, MethodTransaction methodTransaction) {
        if (methodTransaction.inheritance().value() == Mode.NONE) {
            String groupName = methodTransaction.group();

            List<AnnotationTransactionDefList> transactionDefLists = instrumenter.getAnnotationDefinitions().get(DevOpsAnnotationDefinition.METHOD_TRANSACTION_DESCRIPTOR + groupName);
            if (transactionDefLists == null && !groupName.isEmpty()) {
                transactionDefLists = instrumenter.getAnnotationDefinitions().get(DevOpsAnnotationDefinition.METHOD_TRANSACTION_DESCRIPTOR);
            }
            if (transactionDefLists != null) {
                for (AnnotationTransactionDefList transactionDefList : transactionDefLists) {
                    if (transactionDefList.getDefinition().isMethodAnnotation()) {
                        Handler handler = transactionDefList.getHandler(lookupAnnotationDefinitionSite.init(dottedClassName, dottedClassName));
                        if (handler != null) {
                            if (transactionDefList.getDefinition() instanceof DevOpsAnnotationDefinition) {
                                addMethodInterception(interceptionMethod, new DevOpsConcreteMethodInterception(transactionDefList.getDefinition(), handler, methodTransaction, dottedClassName));
                            } else {
                                addMethodInterception(interceptionMethod, new TransactionInterception(transactionDefList.getDefinition(), handler));
                            }
                        }
                    }
                }
            }
            methodAnnotations.add((DevOpsAnnotationDefinition.METHOD_TRANSACTION_DESCRIPTOR + groupName).intern());
        } else {
            DevOpsAnnotationInfo devOpsAnnotationInfo = DevOpsAnnotationInfo.create(methodTransaction, dottedClassName);
            devOpsAnnotationInfo.setDefinedMethods(Collections.singleton(interceptionMethod));
            methodAnnotations.add(devOpsAnnotationInfo);
        }
    }

    private void checkPojoDefinitions(List<PojoTransactionDefList> pojoTransactionDefLists) {
        if (pojoTransactionDefLists != null) {
            for (PojoTransactionDefList pojoTransactionDefList : pojoTransactionDefLists) {
                PojoDefinition pojoDefinition = pojoTransactionDefList.getDefinition();
                if (pojoDefinition.isInterceptSubclasses()) {
                    TriState triState = classFileInfo.isSubclass(pojoDefinition.getDeclaringClassName());
                    if (triState == TriState.TRUE) {
                        PojoHandler handler = pojoTransactionDefList.getHandler(lookupDefinitionSite.init(dottedClassName));
                        if (handler != null) {
                            addMethodInterception(lookupMethod, new TransactionInterception(pojoDefinition, handler));
                        }
                    } else if (triState == TriState.UNDEFINED) {
                        getPartiallyDefinedInfo(true).getUnsureSuperclasses().add(pojoDefinition.getDeclaringClassName());
                    }
                } else if (pojoDefinition.getDeclaringClassName().equals(classFileInfo.getName().replace('/', '.'))) {
                    PojoHandler handler = pojoTransactionDefList.getHandler(lookupDefinitionSite.init(dottedClassName));
                    if (handler != null) {
                        addMethodInterception(lookupMethod, new TransactionInterception(pojoDefinition, handler));
                    }
                }
            }

        }
    }

    @Override
    public void visitEnd() {
        DevOpsAnnotations devOpsAnnotations = redefined ? previousDevOpsAnnotations : this.devOpsAnnotations;
        if (transactionInstrumentation && redefined && previousDevOpsAnnotations != null) {
            for (Entry<InterceptionMethod, MethodTransaction> entry : previousDevOpsAnnotations.getMethodTransactions().entrySet()) {
                handleDevOpsMethod(entry.getKey(), entry.getValue());
            }
            if (previousDevOpsAnnotations.getClassTransaction() != null) {
                handleDevOpsClass(previousDevOpsAnnotations.getClassTransaction());
            }
        }

        for (Entry<String, Set<InterceptionMethod>> entry : inheritableMethodAnnotationsToMethods.entrySet()) {
            instrumenter.setMethodAnnotations(dottedClassName, entry.getKey(), entry.getValue());
        }

        boolean noTransaction = false;
        if (devOpsAnnotations != null) {
            if (devOpsAnnotations.isNoTransaction()) {
                noTransaction = true;
                methodInterceptions.clear();
            } else {
                for (InterceptionMethod interceptionMethod : devOpsAnnotations.getNoTransactionMethods()) {
                    methodInterceptions.remove(interceptionMethod);
                }
            }
            if (publicMethods != null) {
                publicMethods.removeAll(devOpsAnnotations.getNoTransactionMethods());
            }
        }
        classFileInfo.setNoTransaction(noTransaction);
        instrumenter.setDevOpsAnnotations(dottedClassName, devOpsAnnotations);
        if (!classAnnotations.isEmpty()) {
            classFileInfo.setClassAnnotations(classAnnotations.toArray());
        } else {
            classFileInfo.setClassAnnotations(null);
        }
        if (!methodAnnotations.isEmpty()) {
            classFileInfo.setMethodAnnotations(methodAnnotations.toArray());
        } else {
            classFileInfo.setMethodAnnotations(null);
        }
        if (publicMethods != null) {
            if (devOpsClassTransactionInfo != null) {
                Logger.log(Subsystem.INSTRUMENTATION, 6, false, "setting defined methods %s for dev ops transaction\n", publicMethods);
                devOpsClassTransactionInfo.setDefinedMethods(publicMethods);
            }
            for (TransactionDefinition definition : definitionsWithPublicMethods) {
                Logger.log(Subsystem.INSTRUMENTATION, 6, false, "setting defined methods %s for %s\n", publicMethods, definition);
                definition.setDefinedMethods(publicMethods);
            }
            publicMethods = null;
        }
        if (!isInterface() && transactionInstrumentation && !noTransaction) {
            InterceptionClassHierarchyVisitor interceptionClassHierarchyVisitor = new InterceptionClassHierarchyVisitor(instrumenter.getPojoClassDefinitions(), instrumenter.getAnnotationDefinitions(), classInterceptions, devOpsAnnotations, instrumenter);
            classFileInfo.visit(interceptionClassHierarchyVisitor);
            if (!interceptionClassHierarchyVisitor.isFullyDefined()) {
                getPartiallyDefinedInfo(true).setClassInterceptionCount(classInterceptions.size());
            }
        }
    }

    public Set<BaseInterception> getClassInterceptions() {
        return classInterceptions;
    }

    public Map<InterceptionMethod, Set<BaseInterception>> getMethodInterceptions() {
        return methodInterceptions;
    }

}
