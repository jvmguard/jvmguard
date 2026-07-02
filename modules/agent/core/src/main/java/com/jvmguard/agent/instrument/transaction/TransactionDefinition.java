package com.jvmguard.agent.instrument.transaction;

import com.jvmguard.agent.instrument.model.InterceptionMethod;

import java.util.Set;

public abstract class TransactionDefinition {
    protected boolean staticMethods;
    private volatile Set<InterceptionMethod> definedMethods;

    protected TransactionDefinition(boolean staticMethods) {
        this.staticMethods = staticMethods;
    }

    public boolean isProtectedAndPackageMethods() {
        return false;
    }

    public String getDeclaringClassName() {
        return null;
    }

    public final boolean isStaticMethods() {
        return staticMethods;
    }

    public void setDefinedMethods(Set<InterceptionMethod> definedMethods) {
        this.definedMethods = definedMethods;
    }

    public Set<InterceptionMethod> getDefinedMethods() {
        return definedMethods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransactionDefinition)) {
            return false;
        }

        TransactionDefinition that = (TransactionDefinition)o;

        if (staticMethods != that.staticMethods) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return (staticMethods ? 1 : 0);
    }
}
