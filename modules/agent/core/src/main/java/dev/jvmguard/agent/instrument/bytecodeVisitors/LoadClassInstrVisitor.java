package dev.jvmguard.agent.instrument.bytecodeVisitors;

import dev.jvmguard.agent.callee.ClassLoaderCallee;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import static dev.jvmguard.agent.AgentConstants.ASM_VERSION;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class LoadClassInstrVisitor extends ClassVisitor {
    public static final String LOAD_CLASS_METHOD_NAME = "loadClass";
    public static final String LOAD_CLASS_METHOD_SIGNATURE = "(Ljava/lang/String;)Ljava/lang/Class;";
    public static final String LOAD_CLASS_RESOLVE_METHOD_SIGNATURE = "(Ljava/lang/String;Z)Ljava/lang/Class;";

    private static final String JVMGUARD_USE_SYSTEM_NAME = "__jvmguard_useSystemClassLoader";
    private static final String JVMGUARD_USE_SYSTEM_SIG = "(Ljava/lang/Object;Ljava/lang/String;)Z";
    private static final String JVMGUARD_LOAD_CLASS_NAME = "__jvmguard_loadClass";
    private static final String JVMGUARD_LOAD_CLASS_SIG = "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Class;";

    private static final String CLASS_LOADER_CALLEE_CLASS = ClassLoaderCallee.class.getName().replace('.', '/');

    private String className;

    public LoadClassInstrVisitor(ClassVisitor cv, String className) {
        super(ASM_VERSION, cv);
        this.className = className;
    }

    public static boolean isLoadClassMethod(String className, int access, String name, String desc) {
        if ("com/ibm/oti/vm/BootstrapClassLoader".equals(className)) {
            return false;
        }
        return (access & ACC_STATIC) == 0 && name.equals(LOAD_CLASS_METHOD_NAME) && (desc.equals(LOAD_CLASS_METHOD_SIGNATURE) || desc.equals(LOAD_CLASS_RESOLVE_METHOD_SIGNATURE));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (!isLoadClassMethod(className, access, name, desc)) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        } else {
            final boolean resolve = desc.equals(LOAD_CLASS_RESOLVE_METHOD_SIGNATURE);
            return new AdviceAdapter(ASM_VERSION, super.visitMethod(access, name, desc, signature, exceptions), access, name, desc) {
                @Override
                protected void onMethodEnter() {
                    addLoadClassEnter(mv, className, resolve);
                }
            };
        }
    }

    public static void addLoadClassEnter(MethodVisitor mv, String className, boolean resolve) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS_LOADER_CALLEE_CLASS, JVMGUARD_USE_SYSTEM_NAME, JVMGUARD_USE_SYSTEM_SIG, false);
        Label label = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, label);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS_LOADER_CALLEE_CLASS, JVMGUARD_LOAD_CLASS_NAME, JVMGUARD_LOAD_CLASS_SIG, false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(label);
        mv.visitFrame(Opcodes.F_NEW, resolve ? 3 : 2, new Object[] {className, "java/lang/String", Opcodes.INTEGER}, 0, new Object[0]);
    }
}
