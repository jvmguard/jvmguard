package com.jvmguard.agent.instrument.transaction.annotation;

import com.jvmguard.agent.instrument.transaction.TransactionDefinition;

public abstract class AnnotationDefinition extends TransactionDefinition {
    private String name;
    private boolean methodAnnotation;

    public abstract boolean isInheritable();
    public abstract String getUsedAnnotationDescriptor(SearchType searchType);

    public AnnotationDefinition(String name, boolean methodAnnotation, boolean staticMethods) {
        super(staticMethods);
        this.methodAnnotation = methodAnnotation;
        if (name.contains(";")) {
            this.name = name;
        } else {
            this.name = "L" + name.replace('.', '/') + ";";
        }
    }

    public AnnotationDefinition(AnnotationDefinition other) {
        super(other.staticMethods);
        methodAnnotation = other.methodAnnotation;
        name = other.name;
    }

    public String getName() {
        return name;
    }

    public boolean isMethodAnnotation() {
        return methodAnnotation;
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

        AnnotationDefinition that = (AnnotationDefinition)o;

        if (methodAnnotation != that.methodAnnotation) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (methodAnnotation ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }


    public enum SearchType {
        METHOD,
        CLASS,
        INHERITED_CLASS,
        INHERITED_METHOD
    }
}
