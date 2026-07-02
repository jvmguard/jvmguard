package com.jvmguard.agent.instrument.bytecodeVisitors;

import com.jvmguard.agent.callee.PrimitiveCallee;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class BoxingHelper {
    private static final String PRIMITIVE_CALLEE_CLASS = PrimitiveCallee.class.getName().replace('.', '/');

    public static void box(MethodVisitor mv, final Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return;
        }
        if (type == Type.VOID_TYPE) {
            mv.visitInsn(ACONST_NULL);
        } else {
            mv.visitMethodInsn(INVOKESTATIC, PRIMITIVE_CALLEE_CLASS, "__jvmguard_wrap", getWrapperSignature(type), false);
        }
    }

    private static String getWrapperSignature(final Type type) {
        switch (type.getSort()) {
            case Type.BYTE:
                return "(B)Ljava/lang/Byte;";
            case Type.BOOLEAN:
                return "(Z)Ljava/lang/Boolean;";
            case Type.SHORT:
                return "(S)Ljava/lang/Short;";
            case Type.CHAR:
                return "(C)Ljava/lang/Character;";
            case Type.INT:
                return "(I)Ljava/lang/Integer;";
            case Type.FLOAT:
                return "(F)Ljava/lang/Float;";
            case Type.LONG:
                return "(J)Ljava/lang/Long;";
            case Type.DOUBLE:
                return "(D)Ljava/lang/Double;";
        }
        return null;
    }


}
