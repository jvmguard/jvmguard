package com.jvmguard.agent.instrument.interceptions;

import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.agent.callee.Handler;
import com.jvmguard.agent.instrument.classInfo.DevOpsAnnotationInfo;
import com.jvmguard.agent.instrument.transaction.TransactionDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.DevOpsAnnotationDefinition;
import com.jvmguard.annotation.Inheritance.Mode;
import com.jvmguard.annotation.ReentryInhibition;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.Set;

public class DevOpsClassInterception extends DevOpsInterception {

    private final Set<InterceptionMethod> definedMethods;
    private final DevOpsAnnotationInfo devOpsAnnotationInfo;

    public DevOpsClassInterception(TransactionDefinition definition, Set<InterceptionMethod> noTransactionMethods, Handler handler, String declaringClassName, DevOpsAnnotationInfo devOpsAnnotationInfo, Set<InterceptionMethod> definedMethods) {
        super(definition, noTransactionMethods, handler, declaringClassName);
        this.devOpsAnnotationInfo = devOpsAnnotationInfo;
        this.definedMethods = definedMethods;
    }

    @Override
    public DevOpsAnnotationDefinition getDefinition() {
        return (DevOpsAnnotationDefinition)super.getDefinition();
    }

    @Override
    public String getGroup() {
        return devOpsAnnotationInfo.getGroup();
    }

    @Override
    public ReentryInhibition getReentryInhibition() {
        return devOpsAnnotationInfo.getReentryInhibition();
    }

    @Override
    public Annotation getAnnotation() {
        return devOpsAnnotationInfo.getAnnotation();
    }

    @Override
    public boolean isStaticMethods() {
        return devOpsAnnotationInfo.isStaticMethods();
    }

    @Override
    public boolean isProtectedAndPackageMethods() {
        return devOpsAnnotationInfo.isProtectedMethods();
    }

    @Override
    public String getUsedClassName(String instrumentedClassName) {
        return devOpsAnnotationInfo.getInheritance().value() == Mode.WITH_SUPERCLASS_NAME ? declaringClassName : instrumentedClassName;
    }

    @Override
    public NamingResult getNaming(String className, String methodName, boolean thisAvailable, Type[] argumentTypes) {
        return getNaming(devOpsAnnotationInfo.getNaming(), getUsedClassName(className), methodName, thisAvailable, argumentTypes);
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
        DevOpsAnnotationInfo other = ((DevOpsClassInterception)o).devOpsAnnotationInfo;
        return devOpsAnnotationInfo.equals(other);
    }

    @Override
    public String toString() {
        return "DevOpsClassInterception{" +
            "declaringClassName='" + declaringClassName + '\'' +
            ", definedMethods=" + definedMethods +
            ", devOpsTransactionInfo=" + devOpsAnnotationInfo +
            '}';
    }
}
