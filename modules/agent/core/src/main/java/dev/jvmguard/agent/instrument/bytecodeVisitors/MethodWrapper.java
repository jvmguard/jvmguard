package dev.jvmguard.agent.instrument.bytecodeVisitors;

import dev.jvmguard.agent.AgentConstants;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;

import static dev.jvmguard.agent.AgentConstants.ASM_VERSION;
import static org.objectweb.asm.Opcodes.*;

public class MethodWrapper extends ClassVisitor {
    private static final String JAVA_LANG_THROWABLE = "java/lang/Throwable";

    protected boolean nativeInstrumentation;
    private String className;

    public MethodWrapper(ClassVisitor cv, boolean nativeInstrumentation) {
        super(ASM_VERSION, cv);
        this.nativeInstrumentation = nativeInstrumentation;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit((version & 0xFFFF) < 49 ? 49 : version, access, name, signature, superName, interfaces);
    }

    protected MethodVisitor wrap(int access, String name, String desc, String signature, String[] exceptions, WrappingProvider wrappingProvider) {
        if ((access & ACC_NATIVE) > 0) {
            if (nativeInstrumentation) {
                super.visitMethod(access, AgentConstants.NATIVE_METHOD_PREFIX + name, desc, signature, exceptions);
                return new NativeWrapper(super.visitMethod(access ^ ACC_NATIVE, name, desc, signature, exceptions), access, className, name, desc, wrappingProvider);
            } else {
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        } else {
            return new FinallyAdviceAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, className, name, desc, wrappingProvider);
        }
    }


    public static abstract class WrappingProvider {
        protected void onEnter(GeneratorAdapter mv) {
        }

        protected void onReturn(GeneratorAdapter mv, int opcode) {
        }

        protected void onFinalCatch(GeneratorAdapter mv) {
        }

        protected boolean isCatch() {
            return false;
        }

        protected boolean isPassThisForExit() {
            return false;
        }

        protected boolean isPassParametersForExit() {
            return false;
        }
    }

    private static class NativeWrapper extends GeneratorAdapter {
        private final int access;
        private final String className;
        private final String name;
        private final String desc;
        private final WrappingProvider wrappingProvider;

        private NativeWrapper(MethodVisitor mv, int access, String className, String name, String desc, WrappingProvider wrappingProvider) {
            super(ASM_VERSION, mv, access, name, desc);
            this.access = access;
            this.className = className;
            this.name = name;
            this.desc = desc;
            this.wrappingProvider = wrappingProvider;
        }

        @Override
        public void visitEnd() {

            mv.visitCode();
            Label enterLabel = new Label();
            if (wrappingProvider.isCatch()) {
                mv.visitLabel(enterLabel);
            }
            wrappingProvider.onEnter(this);

            if ((access & ACC_STATIC) == 0) {
                mv.visitVarInsn(ALOAD, 0);
            }
            loadArgs();
            mv.visitMethodInsn(getInvokeOpcode(access), className, AgentConstants.NATIVE_METHOD_PREFIX + name, desc, false);
            int returnOpcode = Type.getReturnType(desc).getOpcode(IRETURN);
            wrappingProvider.onReturn(this, returnOpcode);
            mv.visitInsn(returnOpcode);

            if (wrappingProvider.isCatch()) {
                Label exitLabel = new Label();
                mv.visitLabel(exitLabel);
                visitFinallyFrame(mv, access, className, getArgumentTypes(), wrappingProvider.isPassThisForExit(), wrappingProvider.isPassParametersForExit());
                mv.visitTryCatchBlock(enterLabel, exitLabel, exitLabel, null);
                wrappingProvider.onFinalCatch(this);
                mv.visitInsn(ATHROW);
            }

            mv.visitMaxs(0, 0);

            super.visitEnd();
        }

        private int getInvokeOpcode(int access) {
            if ((access & ACC_STATIC) > 0) {
                return INVOKESTATIC;
            } else if ((access & ACC_PRIVATE) > 0) {
                return INVOKESPECIAL;
            } else {
                return INVOKEVIRTUAL;
            }
        }

    }

    private static class FinallyAdviceAdapter extends AdviceAdapter {
        private final WrappingProvider wrappingProvider;
        private final String className;

        private Label enterLabel = new Label();
        private Label exitLabel = new Label();

        public FinallyAdviceAdapter(MethodVisitor mv, int access, String className, String name, String desc, WrappingProvider wrappingProvider) {
            super(ASM_VERSION, new GeneratorAdapter(mv, access, name, desc), access, name, desc);
            this.className = className;
            this.wrappingProvider = wrappingProvider;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            if (wrappingProvider.isCatch()) {
                mv.visitLabel(exitLabel);
                visitFinallyFrame(mv, methodAccess, className, getArgumentTypes(), wrappingProvider.isPassThisForExit(), wrappingProvider.isPassParametersForExit());
                mv.visitTryCatchBlock(enterLabel, exitLabel, exitLabel, JAVA_LANG_THROWABLE);
                wrappingProvider.onFinalCatch((GeneratorAdapter)mv);
                mv.visitInsn(ATHROW);
            }
            super.visitMaxs(maxStack, maxLocals);
        }

        @Override
        protected final void onMethodEnter() {
            if (wrappingProvider.isCatch()) {
                mv.visitLabel(enterLabel);
            }
            wrappingProvider.onEnter((GeneratorAdapter)mv);
        }

        @Override
        protected final void onMethodExit(int opcode) {
            if (opcode != ATHROW) {
                wrappingProvider.onReturn((GeneratorAdapter)mv, opcode);
            }
        }
    }

    private static void visitFinallyFrame(MethodVisitor mv, int methodAccess, String className, Type[] argumentTypes, boolean passThisForExit, boolean passParametersForExit) {
        int localsLength = 0;
        Object[] localsArray = null;
        if (passThisForExit || passParametersForExit) {
            localsArray = new Object[argumentTypes.length + 1];

            if ((methodAccess & Opcodes.ACC_STATIC) == 0) {
                localsArray[localsLength++] = passThisForExit ? className : Opcodes.TOP;
            }
            if (passParametersForExit) {
                for (Type argumentType : argumentTypes) {
                    localsArray[localsLength++] = getFrameStackType(argumentType);
                }
            }
        }
        mv.visitFrame(Opcodes.F_NEW, localsLength, localsArray, 1, new Object[] {JAVA_LANG_THROWABLE});
    }

    private static Object getFrameStackType(Type argumentType) {
        switch (argumentType.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.INTEGER;
            case Type.FLOAT:
                return Opcodes.FLOAT;
            case Type.LONG:
                return Opcodes.LONG;
            case Type.DOUBLE:
                return Opcodes.DOUBLE;
            default:
                return argumentType.getInternalName();
        }
    }
}
