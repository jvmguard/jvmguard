package dev.jvmguard.agent.instrument;

import dev.jvmguard.agent.util.MutableInt;
import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.comm.JvmGuardCommunication;
import dev.jvmguard.agent.config.recording.RetransformationType;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationTransactionDefList;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedDefinition;
import dev.jvmguard.agent.instrument.transaction.matched.MatchedTransactionDefList;
import dev.jvmguard.agent.util.Logger;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;

public class Transformer implements TransformerTarget {

    private static Transformer instance = new Transformer();

    private final Instrumenter instrumenter = new Instrumenter();
    private final ClassFileTransformer wrapper = new TransformWrapper(this);

    private volatile Instrumentation instrumentation;

    private volatile Set<AnnotationDefinition> annotations = new HashSet<>();
    private volatile Set<MatchedDefinition> pojoDefinitions = new HashSet<>();

    private int communicationUpdate = 0;

    // for testing only
    public static RetransformListener retransformListener;

    public static Transformer getInstance() {
        return instance;
    }

    private Transformer() {
        RetransformationUtil.init();
    }

    public void initFeatures() {
        instrumenter.initFeatures();
    }

    public void remove() {
        if (instrumentation != null) {
            instrumentation.removeTransformer(getWrapper());
        }
        TargetClassGenerator.getInstance().removeHandlers();
        instrumenter.freeMemory();
        annotations = new HashSet<>();
        pojoDefinitions = new HashSet<>();
    }

    public Transformer setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        return this;
    }

    public void initLoadedClasses() {
        Collection<Class> retransformClasses = instrumenter.initLoadedClasses(instrumentation);
        if (instrumentation.isModifiableClass(ClassLoader.class)) {
            retransformClasses.add(ClassLoader.class);
        }
        try {
            Class<?> mBeanServerFactory = Class.forName("javax.management.MBeanServerFactory");
            if (instrumentation.isModifiableClass(mBeanServerFactory)) {
                retransformClasses.add(mBeanServerFactory);
            }
        } catch (Throwable ignored) {
        }
        try {
            instrumentation.retransformClasses(retransformClasses.toArray(new Class[0]));
        } catch (UnmodifiableClassException e) {
            Logger.log(Subsystem.INSTRUMENTATION, 0, true, e);
        }

    }

    @Override
    public byte[] transform(Object module, ClassLoader loader, String className, Class classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        try {
            if (className != null) {
                JvmGuardCommunication.checkStart(className, loader);
                return instrumenter.transform(loader, className, classBeingRedefined, classFileBuffer);
            }
        } catch (Throwable t) {
            Logger.log(Subsystem.INSTRUMENTATION, 0, true, "while instrumenting class " + className, t);
        }
        return null;
    }

    public Map<String, MutableInt> getPackageStats() {
        Map<String, MutableInt> packageToCount = new HashMap<>();

        for (Class clazz : getAllLoadedClasses()) {
            if (!clazz.isArray() && !clazz.isPrimitive()) {
                String className = clazz.getName();

                int lastDot = className.lastIndexOf(".");
                String packageName = "";
                if (lastDot > -1) {
                    packageName = className.substring(0, lastDot);
                }
                MutableInt value = packageToCount.computeIfAbsent(packageName, k -> new MutableInt());
                value.val++;
            }
        }
        return packageToCount;
    }

    public Class[] getAllLoadedClasses() {
        return instrumentation.getAllLoadedClasses();
    }

    public void setTransactionDefs(Map<MatchedDefinition, MatchedTransactionDefList> pojoInterceptionMap, Map<AnnotationDefinition, AnnotationTransactionDefList> annotationInterceptionMap, boolean initial, RetransformationType retransformationType) {
        List<Class> retransformClasses = instrumenter.calculateChanges(pojoInterceptionMap, annotationInterceptionMap, annotations, pojoDefinitions, instrumentation);

        annotations = new HashSet<>(annotationInterceptionMap.keySet());
        pojoDefinitions = new HashSet<>(pojoInterceptionMap.keySet());

        if (initial || retransformationType == RetransformationType.ALWAYS || (retransformationType == RetransformationType.FIRST_CONNECTION && communicationUpdate == 0)) {
            Logger.log(Subsystem.INSTRUMENTATION, 2, true, "retransforming %s\n", retransformClasses);
            if (retransformListener != null) {
                retransformListener.retransform(retransformClasses);
            }
            if (!retransformClasses.isEmpty()) {
                try {
                    instrumentation.retransformClasses(retransformClasses.toArray(new Class[0]));
                } catch (UnmodifiableClassException e) {
                    JvmGuardAgent.log(e);
                }
            }
        }
        if (!initial) {
            communicationUpdate++;
        }
    }

    public void registerClass(boolean addedClinit, Class clazz) {
        instrumenter.registerClassLoad(instrumentation, addedClinit, clazz);
    }


    public Class getClass(String className) {
        for (Class clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        return null;
    }

    public Instrumenter getInstrumenter() {
        return instrumenter;
    }

    public ClassFileTransformer getWrapper() {
        return wrapper;
    }

    public interface RetransformListener {
        void retransform(Collection<Class> classes);
    }
}
