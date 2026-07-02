package com.jvmguard.agent.instrument;

import java.lang.Module;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

@SuppressWarnings("Since15")
public class TransformWrapper implements ClassFileTransformer {
    private final TransformerTarget transformerTarget;

    public TransformWrapper(TransformerTarget transformerTarget) {
        this.transformerTarget = transformerTarget;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        return transformerTarget.transform(null, loader, className, classBeingRedefined, protectionDomain, classFileBuffer);
    }

    @Override
    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        return transformerTarget.transform(module, loader, className, classBeingRedefined, protectionDomain, classFileBuffer);
    }
}
