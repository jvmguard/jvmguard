package com.jvmguard.agent.instrument.transaction.pojo;

import com.jvmguard.agent.instrument.transaction.TransactionDefinition;

import java.util.Objects;

public class PojoDefinition extends TransactionDefinition {

    private final String declaringClassName;
    private final boolean interceptSubclasses;

    private final boolean onlyImplementingMethods;

    private final String methodName;
    private final String methodSignature;

    public PojoDefinition(String declaringClassName, boolean interceptSubclasses, boolean onlyImplementingMethods, boolean staticMethods, String methodName, String methodSignature) {
        super(staticMethods);
        this.declaringClassName = declaringClassName;
        this.interceptSubclasses = interceptSubclasses;
        this.onlyImplementingMethods = onlyImplementingMethods;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
    }

    @Override
    public String getDeclaringClassName() {
        return declaringClassName;
    }

    public boolean isInterceptSubclasses() {
        return interceptSubclasses;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public boolean isTransferArguments() {
        return methodSignature != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PojoDefinition that = (PojoDefinition)o;

        if (interceptSubclasses != that.interceptSubclasses) {
            return false;
        }
        if (onlyImplementingMethods != that.onlyImplementingMethods) {
            return false;
        }
        if (!declaringClassName.equals(that.declaringClassName)) {
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
        int result = super.hashCode();
        result = 31 * result + declaringClassName.hashCode();
        result = 31 * result + (interceptSubclasses ? 1 : 0);
        result = 31 * result + (onlyImplementingMethods ? 1 : 0);
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
        result = 31 * result + (methodSignature != null ? methodSignature.hashCode() : 0);
        return result;
    }


    @Override
    public boolean isProtectedAndPackageMethods() {
        return methodName != null;
    }

    public boolean isSuperclassWithImplementingMethods() {
        return methodName == null && interceptSubclasses && onlyImplementingMethods;
    }

    @Override
    public String toString() {
        return "PojoDefinition{" +
            "declaringClassName='" + declaringClassName + '\'' +
            ", interceptSubclasses=" + interceptSubclasses +
            ", onlyImplementingMethods=" + onlyImplementingMethods +
            ", methodName='" + methodName + '\'' +
            ", methodSignature='" + methodSignature + '\'' +
            '}';
    }
}
