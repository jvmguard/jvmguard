package com.jvmguard.agent.instrument;

import com.jvmguard.agent.AgentProperties;
import com.jvmguard.agent.ClassTransformer;
import com.jvmguard.agent.MethodTransformer;
import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.util.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class NameTransformation {

    private static final String CLASS_NAME_TRANSFORMER = "classNameTransformer";
    private static final String CLASS_NAME_DELIMITERS = "classNameDelimiters";
    private static final String METHOD_NAME_TRANSFORMER = "methodNameTransformer";
    public static final String NONE = "none";

    private static final String[] DEFAULT_DELIMITERS = {"_$$_", "$$"};

    private static final ClassTransformer classTransformer = initClassTransformer();
    private static final MethodTransformer methodTransformer = initMethodTransformer();

    private static MethodTransformer initMethodTransformer() {
        try {
            String customTransformerName = AgentProperties.getProperty(METHOD_NAME_TRANSFORMER);
            if (customTransformerName != null) {
                Class<?> transformerClass = Class.forName(customTransformerName, true, ClassLoader.getSystemClassLoader());
                if (MethodTransformer.class.isAssignableFrom(transformerClass)) {
                    return (MethodTransformer)transformerClass.newInstance();
                } else {
                    Method method = transformerClass.getMethod("transformMethod", String.class, String.class);
                    if (!method.getReturnType().equals(String.class)) {
                        throw new IllegalArgumentException("no public String transformMethod(String,String) method in " + customTransformerName);
                    }
                    Object object = null;
                    if (!Modifier.isStatic(method.getModifiers())) {
                        object = transformerClass.newInstance();
                    }
                    return new CustomMethodTransformer(object, method);
                }
            }
        } catch (Throwable e) {
            Logger.log(Subsystem.INSTRUMENTATION, 0, true, e);
        }

        return new NoMethodTransformer();
    }

    private static ClassTransformer initClassTransformer() {
        try {
            String customTransformerName = AgentProperties.getProperty(CLASS_NAME_TRANSFORMER);
            String customDelimiters = AgentProperties.getProperty(CLASS_NAME_DELIMITERS);
            if (NONE.equals(customTransformerName)) {
                return new NoClassTransformer();
            } else if (customTransformerName != null) {
                Class<?> transformerClass = Class.forName(customTransformerName, true, ClassLoader.getSystemClassLoader());
                if (ClassTransformer.class.isAssignableFrom(transformerClass)) {
                    return (ClassTransformer)transformerClass.newInstance();
                } else {
                    Method method = transformerClass.getMethod("transform", String.class);
                    if (!method.getReturnType().equals(String.class)) {
                        throw new IllegalArgumentException("no public String transform(String) method in " + customTransformerName);
                    }
                    Object object = null;
                    if (!Modifier.isStatic(method.getModifiers())) {
                        object = transformerClass.newInstance();
                    }
                    return new CustomClassTransformer(object, method);
                }
            } else if (customDelimiters != null) {
                return new DelimiterClassTransformer(customDelimiters.split(","));
            }
        } catch (Throwable e) {
            Logger.log(Subsystem.INSTRUMENTATION, 0, true, e);
        }

        return new DelimiterClassTransformer(DEFAULT_DELIMITERS);
    }

    public static String transformClass(String className) {
        try {
            return classTransformer.transform(className);
        } catch (Throwable e) {
            Logger.log(Subsystem.INSTRUMENTATION, 0, true, e);
        }
        return className;
    }

    public static String transformMethod(String className, String name, String desc) {
        try {
            return methodTransformer.transformMethod(className, name, desc);
        } catch (Throwable e) {
            Logger.log(Subsystem.INSTRUMENTATION, 0, true, e);
        }
        return className;
    }

    private static class CustomClassTransformer implements ClassTransformer {
        private Object object;
        private Method method;

        private CustomClassTransformer(Object object, Method method) {
            this.object = object;
            this.method = method;
        }

        @Override
        public String transform(String name) throws InvocationTargetException, IllegalAccessException {
            return (String)method.invoke(object, name);
        }
    }

    private static class CustomMethodTransformer implements MethodTransformer {
        private Object object;
        private Method method;

        private CustomMethodTransformer(Object object, Method method) {
            this.object = object;
            this.method = method;
        }

        @Override
        public String transformMethod(String className, String name, String descriptor) throws InvocationTargetException, IllegalAccessException {
            return (String)method.invoke(object, className, name);
        }
    }

    private static class DelimiterClassTransformer implements ClassTransformer {
        private String[] delimiters;

        private DelimiterClassTransformer(String[] delimiters) {
            this.delimiters = delimiters;
        }

        @Override
        public String transform(String name) {
            for (String delimiter : delimiters) {
                int index = name.indexOf(delimiter);
                if (index > -1) {
                    return name.substring(0, index);
                }
            }
            return name;
        }
    }

    private static class NoClassTransformer implements ClassTransformer {
        @Override
        public String transform(String name) {
            return name;
        }
    }

    private static class NoMethodTransformer implements MethodTransformer {
        @Override
        public String transformMethod(String className, String name, String descriptor) {
            return name;
        }
    }

}
