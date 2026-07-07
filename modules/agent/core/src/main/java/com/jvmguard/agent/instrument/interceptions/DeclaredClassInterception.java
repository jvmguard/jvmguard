package com.jvmguard.agent.instrument.interceptions;

import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.agent.callee.Handler;
import com.jvmguard.agent.instrument.classInfo.DeclaredAnnotationInfo;
import com.jvmguard.agent.instrument.transaction.TransactionDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.DeclaredAnnotationDefinition;
import com.jvmguard.annotation.Inheritance.Mode;
import com.jvmguard.annotation.ReentryInhibition;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.Set;

public class DeclaredClassInterception extends DeclaredInterception {

    private final Set<InterceptionMethod> definedMethods;
    private final DeclaredAnnotationInfo declaredAnnotationInfo;

    public DeclaredClassInterception(TransactionDefinition definition, Set<InterceptionMethod> noTransactionMethods, Handler handler, String declaringClassName, DeclaredAnnotationInfo declaredAnnotationInfo, Set<InterceptionMethod> definedMethods) {
        super(definition, noTransactionMethods, handler, declaringClassName);
        this.declaredAnnotationInfo = declaredAnnotationInfo;
        this.definedMethods = definedMethods;
    }

    @Override
    public DeclaredAnnotationDefinition getDefinition() {
        return (DeclaredAnnotationDefinition)super.getDefinition();
    }

    @Override
    public String getGroup() {
        return declaredAnnotationInfo.getGroup();
    }

    @Override
    public ReentryInhibition getReentryInhibition() {
        return declaredAnnotationInfo.getReentryInhibition();
    }

    @Override
    public Annotation getAnnotation() {
        return declaredAnnotationInfo.getAnnotation();
    }

    @Override
    public boolean isStaticMethods() {
        return declaredAnnotationInfo.isStaticMethods();
    }

    @Override
    public boolean isProtectedAndPackageMethods() {
        return declaredAnnotationInfo.isProtectedMethods();
    }

    @Override
    public String getUsedClassName(String instrumentedClassName) {
        return declaredAnnotationInfo.getInheritance().value() == Mode.WITH_SUPERCLASS_NAME ? declaringClassName : instrumentedClassName;
    }

    @Override
    public NamingResult getNaming(String className, String methodName, boolean thisAvailable, Type[] argumentTypes) {
        return getNaming(declaredAnnotationInfo.getNaming(), getUsedClassName(className), methodName, thisAvailable, argumentTypes);
    }

    @Override
    public Set<InterceptionMethod> getDefinedMethods() {
        return definedMethods;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        DeclaredAnnotationInfo other = ((DeclaredClassInterception)o).declaredAnnotationInfo;
        return declaredAnnotationInfo.equals(other);
    }

    @Override
    public String toString() {
        return "DeclaredClassInterception{" +
            "declaringClassName='" + declaringClassName + '\'' +
            ", definedMethods=" + definedMethods +
            ", declaredTransactionInfo=" + declaredAnnotationInfo +
            '}';
    }
}
