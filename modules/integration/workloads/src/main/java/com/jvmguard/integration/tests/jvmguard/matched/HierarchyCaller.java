package com.jvmguard.integration.tests.jvmguard.matched;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class HierarchyCaller {

    public static void call(Class referenceClass) {
        invokeMethods(referenceClass.getPackage().getName() + ".Class");
        invokeMethods(referenceClass.getPackage().getName() + ".SubClass");
    }

    private static void invokeMethods(String baseClassName) {
        try {

            for (int subClassIndex=1;;subClassIndex++) {
                Class subClass = Class.forName(baseClassName + subClassIndex);
                if (!Modifier.isAbstract(subClass.getModifiers()) && !Modifier.isInterface(subClass.getModifiers())) {
                    Object instance = subClass.newInstance();
                    for (Method method : subClass.getMethods()) {
                        if (!method.getDeclaringClass().equals(Object.class)) {
                            Object[] arguments = new Object[method.getParameterTypes().length];
                            for (int argIndex = 0; argIndex < arguments.length; argIndex++) {
                                if (method.getParameterTypes()[argIndex].equals(String.class)) {
                                    arguments[argIndex] = "test string";
                                } else if (method.getParameterTypes()[argIndex].equals(int.class)) {
                                    arguments[argIndex] = 5555;
                                }
                            }
                            try {
                                method.invoke(instance, arguments);
                            } catch (InvocationTargetException ignored) {
                            }
                            //System.out.println("called " + method);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // end reached
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
