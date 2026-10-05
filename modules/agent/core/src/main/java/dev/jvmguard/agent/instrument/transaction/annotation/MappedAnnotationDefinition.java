package dev.jvmguard.agent.instrument.transaction.annotation;

import dev.jvmguard.agent.config.transactions.AnnotationValueFilter;
import dev.jvmguard.agent.config.transactions.TransactionType;

import java.util.Map;
import java.util.Objects;

public class MappedAnnotationDefinition extends AnnotationDefinition {
    private boolean inheritable;
    private boolean implementingOnly;
    private boolean useDeclaringClassName;
    private boolean attributeCapture;
    private AnnotationValueFilter valueFilter;
    private final TransactionType transactionType;

    public MappedAnnotationDefinition(String name, boolean methodAnnotation, boolean staticMethods, TransactionType transactionType) {
        super(name, methodAnnotation, staticMethods);
        this.transactionType = transactionType;
    }

    public MappedAnnotationDefinition inheritable(boolean inheritable) {
        this.inheritable = inheritable;
        return this;
    }

    public MappedAnnotationDefinition implementingOnly(boolean implementingOnly) {
        this.implementingOnly = implementingOnly;
        return this;
    }

    public MappedAnnotationDefinition useDeclaringClassName(boolean useDeclaringClassName) {
        this.useDeclaringClassName = useDeclaringClassName;
        return this;
    }

    public MappedAnnotationDefinition attributeCapture(boolean attributeCapture) {
        this.attributeCapture = attributeCapture;
        return this;
    }

    public MappedAnnotationDefinition valueFilter(AnnotationValueFilter valueFilter) {
        this.valueFilter = valueFilter;
        return this;
    }

    public boolean isAttributeCapture() {
        return attributeCapture || valueFilter != null && valueFilter.isActive();
    }
    public boolean matchesValueFilter(Map<String, String> attributes) {
        return valueFilter == null || valueFilter.matches(attributes);
    }

    public boolean isImplementingOnly() {
        return implementingOnly;
    }

    @Override
    public boolean isInheritable() {
        return inheritable;
    }

    public boolean isUseDeclaringClassName() {
        return useDeclaringClassName;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public String getUsedAnnotationDescriptor(SearchType searchType) {
        if (isMethodAnnotation()) {
            if (searchType == SearchType.INHERITED_METHOD) {
                return isInheritable() ? getName() : null;
            } else if (searchType == SearchType.METHOD) {
                return isInheritable() ? null : getName();
            }
        } else {
            if (searchType == SearchType.INHERITED_CLASS) {
                return isInheritable() ? getName() : null;
            } else if (searchType == SearchType.CLASS) {
                return isInheritable() ? null : getName();
            }
        }
        return null;
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

        MappedAnnotationDefinition that = (MappedAnnotationDefinition)o;

        if (inheritable != that.inheritable) {
            return false;
        }
        if (implementingOnly != that.implementingOnly) {
            return false;
        }
        if (useDeclaringClassName != that.useDeclaringClassName) {
            return false;
        }
        if (attributeCapture != that.attributeCapture) {
            return false;
        }
        if (!Objects.equals(valueFilter, that.valueFilter)) {
            return false;
        }
        if (transactionType != that.transactionType) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (inheritable ? 1 : 0);
        result = 31 * result + (implementingOnly ? 1 : 0);
        result = 31 * result + (useDeclaringClassName ? 1 : 0);
        result = 31 * result + (attributeCapture ? 1 : 0);
        result = 31 * result + Objects.hashCode(valueFilter);
        result = 31 * result + transactionType.hashCode();
        return result;
    }

    public boolean isClassWithImplementingOnly() {
        return isInheritable() && !isMethodAnnotation() && isImplementingOnly();
    }
}
