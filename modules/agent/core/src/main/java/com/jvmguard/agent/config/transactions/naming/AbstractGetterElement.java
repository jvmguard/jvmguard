package com.jvmguard.agent.config.transactions.naming;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.CheckedString;
import com.jvmguard.agent.config.transactions.EnvironmentException;
import com.jvmguard.agent.config.transactions.NamingElement;
import com.jvmguard.agent.config.transactions.naming.ClassNameElement.PackageMode;
import com.jvmguard.agent.instrument.NameTransformation;
import com.jvmguard.agent.util.Logger;
import com.jvmguard.agent.util.reflection.FieldInfo;
import com.jvmguard.agent.util.reflection.MethodInfo;
import com.jvmguard.agent.util.reflection.ReflectionUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;

public abstract class AbstractGetterElement extends NamingElement {

    private static final String SPECIAL_SIMPLE_FIELD = "simpleName";
    private static final String SPECIAL_ABBREV_FIELD = "abbrevName";

    private CheckedString getterChain = new CheckedString();

    private transient Getter[] getters;

    protected AbstractGetterElement() {
    }

    protected AbstractGetterElement(String getterChain) {
        this.getterChain.setUsedValue(getterChain);
    }

    @Override
    public boolean canBeStatic() {
        return false;
    }

    @Override
    public boolean isIdentical(NamingElement namingElement) {
        if (!super.isIdentical(namingElement)) {
            return false;
        }
        AbstractGetterElement other = (AbstractGetterElement)namingElement;
        return getterChain.equals(other.getterChain);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
        readState(new BinaryAgentReader(in));
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
        writeState(new BinaryAgentWriter(out));
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        getterChain = reader.readCheckedString("getterChain");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeCheckedString("getterChain", getterChain);
    }

    public CheckedString getGetterChain() {
        return getterChain;
    }

    protected void appendGetterChain(StringBuilder buffer) {
        if (!getterChain.getUsedValue().isEmpty()) {
            buffer.append(", getter chain: \"");
            buffer.append(getterChain.getUsedValue());
            buffer.append("\"");
        }
    }

    private Getter[] getGetters() {
        if (getters == null) {
            getters = readGetters(getterChain.getUsedValue());
        }
        return getters;
    }

    protected void appendName(StringBuilder buffer, Object object) throws EnvironmentException {
        try {
            buffer.append(invokeGetters(getGetters(), object));
        } catch (Throwable e) {
            Logger.log(Subsystem.INSTRUMENTATION, 10, true, e);
            try {
                if (Logger.isEnabled(Subsystem.USER, 10)) {
                    Logger.log(Subsystem.USER, 10, true, "error inserting dynamic naming part '%s' on %s\n", getterChain.getUsedValue(), object);
                    Logger.log(Subsystem.USER, 10, true, e);
                }
            } catch (Throwable ignored) {
            }
            // continue with the next entry
        }
    }

    public static Getter[] readGetters(String getterString) {
        String[] getterStrings = getterString.split("\\.");
        Getter[] getter = new Getter[getterStrings.length];
        for (int getterIndex = 0; getterIndex < getterStrings.length; getterIndex++) {
            String current = getterStrings[getterIndex];
            getter[getterIndex] = new Getter();
            if (current.endsWith("()")) {
                getter[getterIndex].name = current.substring(0, current.length() - 2);
            } else {
                if (SPECIAL_ABBREV_FIELD.equals(current)) {
                    getter[getterIndex].getterType = GetterType.SPECIAL_ABBREV;
                } else if (SPECIAL_SIMPLE_FIELD.equals(current)) {
                    getter[getterIndex].getterType = GetterType.SPECIAL_SIMPLE;
                } else {
                    getter[getterIndex].getterType = GetterType.FIELD;
                }
                getter[getterIndex].name = current;
            }
        }
        if (getter.length >= 2 && getter[getter.length - 2].isMethod("getClass")) {
            if (getter[getter.length - 1].isMethod("getName")) {
                getter[getter.length - 2] = new ClassNameGetter(PackageMode.FULL);
            } else if (getter[getter.length - 1].getterType == GetterType.SPECIAL_SIMPLE) {
                getter[getter.length - 2] = new ClassNameGetter(PackageMode.NONE);
            } else if (getter[getter.length - 1].getterType == GetterType.SPECIAL_ABBREV) {
                getter[getter.length - 2] = new ClassNameGetter(PackageMode.ABBREVIATED);
            }
        }
        return getter;
    }

    public static Object invokeGetters(Getter[] getters, Object obj) throws Exception {
        if (getters != null) {
            for (int getterIndex = 0; getterIndex < getters.length && obj != null; getterIndex++) {
                Getter getter = getters[getterIndex];
                if (getter instanceof ClassNameGetter) {
                    return ((ClassNameGetter)getter).getName(obj);
                } else if (!getter.name.isEmpty()) {
                    if (getter.getterType == GetterType.METHOD) {
                        try {
                            Method method = obj.getClass().getMethod(getter.name, ReflectionUtil.EMPTY_CLASSES);
                            obj = method.invoke(obj, ReflectionUtil.EMPTY_OBJECTS);
                        } catch (NoSuchMethodException e) {
                            MethodInfo methodInfo = ReflectionUtil.getAccessibleMethod(obj.getClass(), getter.name, ReflectionUtil.EMPTY_CLASSES);
                            if (methodInfo.method == null) {
                                throw e;
                            }
                            obj = methodInfo.invokeIfPossible(obj, ReflectionUtil.EMPTY_OBJECTS);
                        }
                    } else if (getter.getterType == GetterType.SPECIAL_SIMPLE && obj instanceof Class) {
                        obj = getClassName(NameTransformation.transformClass(((Class)obj).getName()), PackageMode.NONE);
                    } else if (getter.getterType == GetterType.SPECIAL_ABBREV && obj instanceof Class) {
                        obj = getClassName(NameTransformation.transformClass(((Class)obj).getName()), PackageMode.ABBREVIATED);
                    } else {
                        FieldInfo fieldInfo = ReflectionUtil.getAccessibleField(obj.getClass(), getter.name);
                        if (fieldInfo.field == null) {
                            throw new NoSuchFieldException(obj.getClass().getName() + "." + getter.name);
                        }
                        obj = fieldInfo.getIfPossible(obj);
                    }
                }
            }
        }
        return obj;
    }

    private static String getClassName(String className, PackageMode packageMode) {
        if (packageMode == PackageMode.FULL) {
            return className;
        } else {
            int dotIndex = className.lastIndexOf('.');
            if (dotIndex < 0) {
                return className;
            } else {
                if (packageMode == PackageMode.NONE) {
                    return className.substring(dotIndex + 1);
                } else {
                    StringBuilder buffer = new StringBuilder();
                    boolean appendNext = true;
                    for (int i = 0; i < dotIndex; i++) {
                        char c = className.charAt(i);
                        if (c == '.') {
                            buffer.append('.');
                            appendNext = true;
                        } else if (appendNext) {
                            buffer.append(c);
                            appendNext = false;
                        }
                    }
                    buffer.append(className, dotIndex, className.length());
                    return buffer.toString();
                }
            }
        }
    }

    public static class ClassNameGetter extends Getter {
        final PackageMode packageMode;

        public ClassNameGetter(PackageMode packageMode) {
            this.packageMode = packageMode;
        }

        public Object getName(Object obj) {
            return getClassName(NameTransformation.transformClass(obj.getClass().getName()), packageMode);
        }
    }

    private enum GetterType {
        METHOD, FIELD, SPECIAL_SIMPLE, SPECIAL_ABBREV
    }

    public static class Getter {
        private String name;
        private GetterType getterType = GetterType.METHOD;

        @Override
        public String toString() {
            return "Getter{" +
                "name='" + name + '\'' +
                ", getterType=" + getterType +
                '}';
        }

        public String getName() {
            return name;
        }

        public boolean isMethod(String name) {
            return getterType == GetterType.METHOD && name.equals(this.name);
        }

    }
}
