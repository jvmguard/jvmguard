package com.jvmguard.agent.instrument.interceptions;

import com.jvmguard.agent.callee.Handler;
import com.jvmguard.agent.instrument.transaction.TransactionDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.DeclaredAnnotationDefinition;
import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.ReentryInhibition;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;

public class DeclaredConcreteMethodInterception extends DeclaredInterception {

    private final MethodTransaction methodTransaction;

    public DeclaredConcreteMethodInterception(TransactionDefinition definition, Handler handler, MethodTransaction methodTransaction, String declaringClassName) {
        super(definition, null, handler, declaringClassName);
        this.methodTransaction = methodTransaction;
    }

    @Override
    public DeclaredAnnotationDefinition getDefinition() {
        return (DeclaredAnnotationDefinition)super.getDefinition();
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
    public boolean isStaticMethods() {
        return true;
    }

    @Override
    public boolean isProtectedAndPackageMethods() {
        return true;
    }

    @Override
    public Annotation getAnnotation() {
        return methodTransaction;
    }

    @Override
    public NamingResult getNaming(String className, String methodName, boolean thisAvailable, Type[] argumentTypes) {
        return getNaming(methodTransaction.naming(), className, methodName, thisAvailable, argumentTypes);
    }

    @Override
    public String toString() {
        return "DeclaredConcreteMethodInterception{" +
            "methodTransaction=" + methodTransaction +
            '}';
    }
}
