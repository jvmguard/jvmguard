package com.jvmguard.agent.instrument.bytecodeVisitors;

import com.jvmguard.agent.instrument.model.InterceptionMethod;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

public class GenericMethodWrapper extends MethodWrapper {
    private final Map<InterceptionMethod, WrappingProvider> methodToProvider;
    private final Set<String> classes;

    private String className;
    private final InterceptionMethod lookupMethod = new InterceptionMethod(null, null);

    private GenericMethodWrapper(ClassVisitor cv, boolean nativeInstrumentation, Map<InterceptionMethod, WrappingProvider> methodToProvider, Set<String> classes) {
        super(cv, nativeInstrumentation);
        this.methodToProvider = methodToProvider;
        this.classes = classes;
    }

    public GenericMethodWrapper(ClassVisitor cv, boolean nativeInstrumentation) {
        this(cv, nativeInstrumentation, Collections.synchronizedMap(new HashMap<>()), Collections.synchronizedSet(new HashSet<>()));
    }

    public GenericMethodWrapper() {
        this(null, false);
    }

    public GenericMethodWrapper(ClassVisitor cv, boolean nativeInstrumentation, GenericMethodWrapper genericMethodWrapper) {
        this(cv, nativeInstrumentation, genericMethodWrapper.methodToProvider, genericMethodWrapper.classes);
    }

    public void addDefinition(InterceptionMethod method, WrappingProvider wrappingProvider) {
        methodToProvider.put(method, wrappingProvider);
        classes.add(method.getClassName());
    }

    public boolean isInstrumented(String className) {
        return classes.contains(className);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        WrappingProvider wrappingProvider = getWrappingProvider(name, desc);
        if (wrappingProvider != null) {
            return wrap(access, name, desc, signature, exceptions, wrappingProvider);
        } else {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private WrappingProvider getWrappingProvider(String name, String desc) {
        WrappingProvider wrappingProvider = methodToProvider.get(lookupMethod.init(className, name, desc));
        if (wrappingProvider == null) {
            wrappingProvider = methodToProvider.get(lookupMethod.init(className, name, null));
        }
        if (wrappingProvider == null) {
            wrappingProvider = methodToProvider.get(lookupMethod.init(className, null, null));
        }
        return wrappingProvider;
    }

}
