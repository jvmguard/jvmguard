package com.jvmguard.agent.instrument.model;

import java.util.Objects;

/**
 * Identifies a method to instrument by class name, method name and JVM method signature. A {@code null}
 * class name denotes a wildcard interception (matched by method name/signature across classes).
 */
public class InterceptionMethod {

    private String className;
    private String methodName;
    protected String methodSignature;

    public InterceptionMethod(String className, String methodName, String methodSignature) {
        if (className != null) {
            className = className.replace('.', '/');
        }
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
    }

    public InterceptionMethod(String methodName, String methodSignature) {
        this(null, methodName, methodSignature);
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public InterceptionMethod init(String className, String methodName, String methodSignature) {
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InterceptionMethod)) {
            return false;
        }

        InterceptionMethod that = (InterceptionMethod)o;

        if (!Objects.equals(className, that.className)) {
            return false;
        }
        if (!Objects.equals(methodName, that.methodName)) {
            return false;
        }
        if (!Objects.equals(methodSignature, that.methodSignature)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
        result = 31 * result + (methodSignature != null ? methodSignature.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "InterceptionMethod{" +
            "className='" + className + '\'' +
            ", methodName='" + methodName + '\'' +
            ", methodSignature='" + methodSignature + '\'' +
            '}';
    }
}
