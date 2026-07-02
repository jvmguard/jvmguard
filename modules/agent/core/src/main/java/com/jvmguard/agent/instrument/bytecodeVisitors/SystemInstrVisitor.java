package com.jvmguard.agent.instrument.bytecodeVisitors;

import com.jvmguard.agent.callee.MBeanCallee;
import com.jvmguard.agent.callee.SystemCallee;
import com.jvmguard.agent.instrument.model.InterceptionMethod;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;

import static org.objectweb.asm.Opcodes.*;

public class SystemInstrVisitor extends GenericMethodWrapper {
    public static final String CALLEE_CLASS_NAME = SystemCallee.class.getName().replace('.', '/');
    private static final String MBEAN_CALLEE_CLASS_NAME = MBeanCallee.class.getName().replace('.', '/');

    public static final String REGISTER_NAME = "__jvmguard_register";

    private static final String CLASS_LOADER_NAME = "java/lang/ClassLoader";
    public static final String MBEAN_SERVER_FACTORY_NAME = "javax/management/MBeanServerFactory";

    public SystemInstrVisitor(ClassVisitor cv, boolean nativeInstrumentation, SystemInstrVisitor systemInstrVisitor) {
        super(cv, nativeInstrumentation, systemInstrVisitor);
    }

    public SystemInstrVisitor() {
        addClassLoaderMethod();

        addDefinition(new InterceptionMethod("jdk/internal/event/VirtualThreadEndEvent", "isTurnedOn", "()Z"), new WrappingProvider() {
            @Override
            protected void onEnter(GeneratorAdapter mv) {
                mv.visitMethodInsn(INVOKESTATIC, CALLEE_CLASS_NAME, "__jvmguard_vthreadEnd", "()V", false);
            }
        });

        // Register application-created (non-platform) MBean servers for browsing. Instrumenting the final
        // MBeanServerFactory (rather than MBeanServerBuilder) catches every created server, including those
        // made by a custom MBeanServerBuilder subclass. The null signature matches all overloads;
        // MBeanManager.addServer deduplicates, so nested/repeated registrations are harmless.
        WrappingProvider registerCreatedServer = new WrappingProvider() {
            @Override
            protected void onReturn(GeneratorAdapter mv, int opcode) {
                if (opcode == ARETURN) {
                    mv.dup();
                    mv.visitMethodInsn(INVOKESTATIC, MBEAN_CALLEE_CLASS_NAME, "__jvmguard_mBeanServerCreated", "(Ljavax/management/MBeanServer;)V", false);
                }
            }
        };
        addDefinition(new InterceptionMethod(MBEAN_SERVER_FACTORY_NAME, "newMBeanServer", null), registerCreatedServer);
        addDefinition(new InterceptionMethod(MBEAN_SERVER_FACTORY_NAME, "createMBeanServer", null), registerCreatedServer);
    }

    private void addClassLoaderMethod() {
        addDefinition(new InterceptionMethod(CLASS_LOADER_NAME, "postDefineClass", "(Ljava/lang/Class;Ljava/security/ProtectionDomain;)V"), new WrappingProvider() {
            @Override
            public void onEnter(GeneratorAdapter mv) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESTATIC, CALLEE_CLASS_NAME, REGISTER_NAME, "(Ljava/lang/Class;)V", false);
            }
        });
        addDefinition(new InterceptionMethod(CLASS_LOADER_NAME, LoadClassInstrVisitor.LOAD_CLASS_METHOD_NAME, LoadClassInstrVisitor.LOAD_CLASS_METHOD_SIGNATURE), new WrappingProvider() {
            @Override
            public void onEnter(GeneratorAdapter mv) {
                LoadClassInstrVisitor.addLoadClassEnter(mv, CLASS_LOADER_NAME, false);
            }
        });
        addDefinition(new InterceptionMethod(CLASS_LOADER_NAME, LoadClassInstrVisitor.LOAD_CLASS_METHOD_NAME, LoadClassInstrVisitor.LOAD_CLASS_RESOLVE_METHOD_SIGNATURE), new WrappingProvider() {
            @Override
            public void onEnter(GeneratorAdapter mv) {
                LoadClassInstrVisitor.addLoadClassEnter(mv, CLASS_LOADER_NAME, true);
            }
        });
    }
}
