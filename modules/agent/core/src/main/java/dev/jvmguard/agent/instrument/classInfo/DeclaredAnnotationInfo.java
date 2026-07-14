package dev.jvmguard.agent.instrument.classInfo;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.helper.matcher.RegexPatternMatcher;
import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.instrument.transaction.annotation.DeclaredAnnotationDefinition;
import dev.jvmguard.annotation.*;
import dev.jvmguard.annotation.Inheritance.FilterType;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class DeclaredAnnotationInfo {
    private final Pattern pattern;
    private final String declaringClass;
    private volatile Set<InterceptionMethod> definedMethods;

    public static DeclaredAnnotationInfo create(final ClassTransaction classTransaction, String declaringClass) {
        return new DeclaredAnnotationInfo(classTransaction.inheritance(), declaringClass) {
            @Override
            public Inheritance getInheritance() {
                return classTransaction.inheritance();
            }

            @Override
            public Part[] getNaming() {
                return classTransaction.naming();
            }

            @Override
            public String getGroup() {
                return classTransaction.group();
            }

            @Override
            public ReentryInhibition getReentryInhibition() {
                return classTransaction.reentryInhibition();
            }

            @Override
            protected boolean isImplementingOnly() {
                return getInheritance().implementingOnly();
            }

            @Override
            public String getAnnotationDescriptor() {
                return DeclaredAnnotationDefinition.CLASS_TRANSACTION_DESCRIPTOR + getGroup();
            }

            @Override
            public Annotation getAnnotation() {
                return classTransaction;
            }

            @Override
            public boolean isStaticMethods() {
                return classTransaction.staticMethods();
            }

            @Override
            public boolean isProtectedMethods() {
                return false;
            }
        };
    }

    public static DeclaredAnnotationInfo create(final MethodTransaction methodTransaction, String declaringClass) {
        return new DeclaredAnnotationInfo(methodTransaction.inheritance(), declaringClass) {
            @Override
            public Inheritance getInheritance() {
                return methodTransaction.inheritance();
            }

            @Override
            public Part[] getNaming() {
                return methodTransaction.naming();
            }

            @Override
            public String getGroup() {
                return methodTransaction.group();
            }

            @Override
            public ReentryInhibition getReentryInhibition() {
                return methodTransaction.reentryInhibition();
            }

            @Override
            protected boolean isImplementingOnly() {
                return true;
            }

            @Override
            public String getAnnotationDescriptor() {
                return DeclaredAnnotationDefinition.METHOD_TRANSACTION_DESCRIPTOR + getGroup();
            }

            @Override
            public Annotation getAnnotation() {
                return methodTransaction;
            }

            @Override
            public boolean isStaticMethods() {
                return true;
            }

            @Override
            public boolean isProtectedMethods() {
                return true;
            }
        };
    }

    public abstract Inheritance getInheritance();
    public abstract Part[] getNaming();
    public abstract String getGroup();
    public abstract ReentryInhibition getReentryInhibition();
    public abstract Annotation getAnnotation();
    protected abstract boolean isImplementingOnly();
    public abstract String getAnnotationDescriptor();
    public abstract boolean isStaticMethods();
    public abstract boolean isProtectedMethods();

    public DeclaredAnnotationInfo(Inheritance inheritance, String declaringClass) {
        this.declaringClass = declaringClass.replace('/', '.');
        Pattern pattern;
        try {
            String regex = inheritance.filter();
            if (inheritance.filterType() == FilterType.WILDCARD) {
                if (regex.equals("*")) {
                    regex = null;
                } else {
                    regex = RegexPatternMatcher.convertToWildcardFilter(inheritance.filter(), true);
                }
            }

            pattern = regex == null ? null : Pattern.compile(regex);
        } catch (Throwable t) {
            JvmGuardAgent.log(t);
            pattern = null;
        }
        this.pattern = pattern;
    }

    public boolean matches(String dottedClassName) {
        try {
            if (pattern != null) {
                return dottedClassName.equals(declaringClass) || pattern.matcher(dottedClassName).matches();
            }
        } catch (Throwable t) {
            JvmGuardAgent.log(t);
        }
        return true;
    }


    public void setDefinedMethods(Set<InterceptionMethod> publicMethods) {
        if (isImplementingOnly()) {
            this.definedMethods = publicMethods;
        }
    }

    public Set<InterceptionMethod> getDefinedMethods() {
        return definedMethods;
    }

    @Override
    public String toString() {
        return "DeclaredClassTransactionInfo{" +
            "annotation=" + getAnnotation() +
            ", pattern=" + pattern +
            ", definedMethods=" + definedMethods +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeclaredAnnotationInfo)) {
            return false;
        }

        DeclaredAnnotationInfo that = (DeclaredAnnotationInfo)o;

        if (!getAnnotation().equals(that.getAnnotation())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getAnnotation().hashCode();
    }
}
