package com.jvmguard.agent.util.reflection;

import com.jvmguard.agent.util.ModuleHelper;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class ReflectionUtil {

    public static final Class[] EMPTY_CLASSES = new Class[0];
    public static final Object[] EMPTY_OBJECTS = new Object[0];

    public static FieldInfo getAccessibleField(Class clazz, String fieldName) {
        Class nextClass = clazz;
        while (nextClass != null) {
            try {
                Field field = nextClass.getDeclaredField(fieldName);
                try {
                    setAccessible(field, true);
                    return new FieldInfo(field);
                } catch (SecurityException e) {
                    return new FieldInfo(null);
                }
            } catch (NoSuchFieldException e) {
                nextClass = nextClass.getSuperclass();
            }
        }
        return new FieldInfo(null);
    }

    @SuppressWarnings("unchecked")
    public static MethodInfo getAccessibleMethod(Class clazz, String methodName, Class... parameters) {
        Class nextClass = clazz;
        while (nextClass != null) {
            try {
                Method method = nextClass.getDeclaredMethod(methodName, parameters);
                try {
                    setAccessible(method, true);
                    return new MethodInfo(method);
                } catch (SecurityException e) {
                    return new MethodInfo(null);
                }
            } catch (NoSuchMethodException e) {
                nextClass = nextClass.getSuperclass();
            }
        }
        return new MethodInfo(null);
    }

    public static <T extends AccessibleObject & Member> T setAccessible(T accessibleMember) {
        setAccessible(accessibleMember, true);
        return accessibleMember;
    }

    public static void setAccessible(AccessibleObject accessibleObject, boolean flag) {
        if (accessibleObject instanceof Member) {
            Class<?> declaringClass = ((Member)accessibleObject).getDeclaringClass();
            String packageName = getPackageName(declaringClass);
            if (packageName != null) {
                ModuleHelper.addOpens(declaringClass, packageName);
            }
        }
        accessibleObject.setAccessible(flag);
    }

    private static String getPackageName(Class<?> clazz) {
        String name = clazz.getName();
        int i = name.lastIndexOf('.');
        if (i != -1) {
            return name.substring(0, i);
        } else {
            return null;
        }
    }
}
