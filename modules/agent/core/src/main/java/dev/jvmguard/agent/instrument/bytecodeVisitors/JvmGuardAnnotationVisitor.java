package dev.jvmguard.agent.instrument.bytecodeVisitors;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.instrument.Proxy;
import dev.jvmguard.agent.util.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.jvmguard.agent.AgentConstants.ASM_VERSION;

public class JvmGuardAnnotationVisitor extends AnnotationVisitor {

    private final ResultVisitor resultVisitor;
    private Annotation annotation;
    private AnnotationInvocationHandler invocationHandler;

    public <T extends Annotation> JvmGuardAnnotationVisitor(Class<? extends T> annotationInterface, ResultVisitor<T> resultVisitor) {
        super(ASM_VERSION);
        this.resultVisitor = resultVisitor;

        try {
            if (annotationInterface.isAnnotation()) {
                invocationHandler = new AnnotationInvocationHandler(annotationInterface);
                annotation = createProxy(annotationInterface, invocationHandler);
            }

        } catch (Throwable e) {
            logException(0, e);
        }
    }

    private <T extends Annotation> T createProxy(Class<T> annotationInterface, InvocationHandler invocationHandler) {
        return Proxy.getProxy(annotationInterface, invocationHandler);
    }

    private void logException(int level, Throwable e) {
        if (level == 0) {
            JvmGuardAgent.log(e);
        } else {
            Logger.log(Subsystem.COMMON, level, true, e);
        }
    }

    private <T extends Annotation> JvmGuardAnnotationVisitor createNew(Class<T> annotationInterface, ResultVisitor<T> resultVisitor) {
        return new JvmGuardAnnotationVisitor(annotationInterface, resultVisitor);
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    @Override
    public void visit(String name, Object value) {
        if (annotation != null) {
            invocationHandler.addValue(name, value);
        }
    }

    @Override
    public AnnotationVisitor visitArray(final String arrayName) {
        if (annotation != null) {
            try {
                final Class arrayType = annotation.getClass().getMethod(arrayName).getReturnType();
                if (arrayType.isArray()) {
                    return new AnnotationVisitor(ASM_VERSION) {
                        List<Object> values = new ArrayList<>();

                        @Override
                        public void visit(String name, Object value) {
                            values.add(value);
                        }

                        @Override
                        public void visitEnum(String name, String desc, String value) {
                            Object enumValue = getEnum(getMemberClass(annotation, arrayName), value);
                            if (enumValue != null) {
                                values.add(enumValue);
                            }
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        public AnnotationVisitor visitAnnotation(String name, String desc) {
                            Class clazz = getMemberClass(annotation, arrayName);
                            if (clazz != null) {
                                return createNew(clazz, annotation -> values.add(annotation));
                            }
                            return null;
                        }

                        @Override
                        public void visitEnd() {
                            invocationHandler.addValue(arrayName, values.toArray((Object[])Array.newInstance(arrayType.getComponentType(), values.size())));
                        }
                    };
                }
            } catch (NoSuchMethodException e) {
                // different class version
            }

        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AnnotationVisitor visitAnnotation(final String name, String desc) {
        Class clazz = getMemberClass(annotation, name);
        if (clazz != null) {
            return createNew(clazz, annotation -> invocationHandler.addValue(name, annotation));
        }
        return null;
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        Object enumValue = getEnum(getMemberClass(annotation, name), value);
        if (enumValue != null) {
            invocationHandler.addValue(name, enumValue);
        }
    }

    private Class getMemberClass(Annotation annotation, String name) {
        if (annotation != null) {
            try {
                Class clazz = annotation.getClass().getMethod(name).getReturnType();
                if (clazz.isArray()) {
                    clazz = clazz.getComponentType();
                }
                return clazz;
            } catch (Throwable e) {
                logException(1, e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object getEnum(Class enumClass, String value) {
        if (enumClass != null) {
            try {
                if (enumClass.isEnum()) {
                    return Enum.valueOf(enumClass, value);
                }
            } catch (IllegalArgumentException e) {
                // unknown enum constant, use default
            } catch (Throwable e) {
                logException(1, e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visitEnd() {
        if (annotation != null) {
            resultVisitor.visit(annotation);
        }
    }

    public static class AnnotationInvocationHandler implements InvocationHandler {

        private static final Object NULL = new Object();

        private Map<String, Object> values = new HashMap<>();

        private final Class<? extends Annotation> annotationInterface;

        private ClassLoader classLoader;

        public AnnotationInvocationHandler(Class<? extends Annotation> annotationInterface) {
            this.annotationInterface = annotationInterface;
            this.classLoader = ClassLoader.getSystemClassLoader();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "toString":
                    return annotationInterface.getName() + "{" + values + "}";
                case "hashCode":
                    return hashCode();
                case "equals":
                    if (args.length == 1) {
                        return proxy == args[0];
                    } else {
                        return false;
                    }
            }

            Object value = values.get(method.getName());
            if (value == null) {
                try {
                    value = method.getDefaultValue();
                } catch (Throwable ignored) {
                }
                if (value == null) {
                    value = NULL;
                }
                values.put(method.getName(), value);
            } else if (value instanceof Type) {
                String className = ((Type)value).getClassName();
                if (classLoader instanceof ClassNameReceiver) {
                    ((ClassNameReceiver)classLoader).handleClassName(className);
                }
                value = Class.forName(className, false, classLoader);
                values.put(method.getName(), value);
            }
            return value == NULL ? null : value;
        }

        public void addValue(String name, Object value) {
            values.put(name, value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AnnotationInvocationHandler that = (AnnotationInvocationHandler)o;

            if (!values.equals(that.values)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return values.hashCode();
        }
    }

    public interface ResultVisitor<T extends Annotation> {
        void visit(T annotation);
    }

    public interface ClassNameReceiver {
        void handleClassName(String className);
    }
}
