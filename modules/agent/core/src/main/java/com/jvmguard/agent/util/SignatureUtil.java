package com.jvmguard.agent.util;

import java.lang.reflect.Method;

public class SignatureUtil {

    public static String getSignature(Class type) {
        if (type.isArray()) {
            return type.getName().replace('.', '/');
        } else if (type.isPrimitive()) {
            if (Void.TYPE.equals(type)) {
                return "V";
            } else if (Boolean.TYPE.equals(type)) {
                return "Z";
            } else if (Character.TYPE.equals(type)) {
                return "C";
            } else if (Byte.TYPE.equals(type)) {
                return "B";
            } else if (Short.TYPE.equals(type)) {
                return "S";
            } else if (Integer.TYPE.equals(type)) {
                return "I";
            } else if (Long.TYPE.equals(type)) {
                return "J";
            } else if (Float.TYPE.equals(type)) {
                return "F";
            } else if (Double.TYPE.equals(type)) {
                return "D";
            }
        }
        return "L" + type.getName().replace('.', '/') + ";";
    }

    public static String getSignature(Class returnType, Class[] paramaterTypes) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (Class parameterType : paramaterTypes) {
            builder.append(getSignature(parameterType));
        }
        builder.append(")");
        builder.append(getSignature(returnType));
        return builder.toString();

    }

    public static String getSignature(Method method) {
        return getSignature(method.getReturnType(), method.getParameterTypes());
    }
}
