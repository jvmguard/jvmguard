package dev.jvmguard.agent.instrument.interceptions;

import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.callee.Handler;
import dev.jvmguard.agent.instrument.transaction.TransactionDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.MappedAnnotationDefinition;

import java.util.Map;
import java.util.Set;

public class AnnotationInterception extends TransactionInterception {

    protected final String declaringClassName;
    private Set<InterceptionMethod> instanceDefinedMethods;

    public AnnotationInterception(TransactionDefinition definition, Set<InterceptionMethod> noTransactionMethods, Handler handler, String declaringClassName) {
        this(definition, noTransactionMethods, handler, declaringClassName, null);
    }

    public AnnotationInterception(TransactionDefinition definition, Set<InterceptionMethod> noTransactionMethods, Handler handler, String declaringClassName, Map<String, String> annotationAttributes) {
        super(definition, noTransactionMethods, handler, annotationAttributes);
        this.declaringClassName = declaringClassName.replace('/', '.');
    }

    public AnnotationInterception(TransactionDefinition definition, Set<InterceptionMethod> noTransactionMethods, Handler handler, String declaringClassName, Map<String, String> annotationAttributes, Set<InterceptionMethod> definedMethods) {
        this(definition, noTransactionMethods, handler, declaringClassName, annotationAttributes);
        this.instanceDefinedMethods = definedMethods;
    }

    /**
     * Restricts this class-level interception to the given methods, overriding the method set of the
     * shared definition. Used for inheritable method annotations whose attributes differ per method
     * and for narrowing the class-level OTel interception of the dedup rule.
     */
    public void restrictToMethods(Set<InterceptionMethod> definedMethods) {
        this.instanceDefinedMethods = definedMethods;
    }

    @Override
    public Set<InterceptionMethod> getDefinedMethods() {
        return instanceDefinedMethods != null ? instanceDefinedMethods : super.getDefinedMethods();
    }

    @Override
    public String getDeclaringClassName() {
        return declaringClassName;
    }

    @Override
    public String getUsedClassName(String instrumentedClassName) {
        boolean declaringClassName = getDefinition() instanceof MappedAnnotationDefinition ? ((MappedAnnotationDefinition)getDefinition()).isUseDeclaringClassName() : false;
        return declaringClassName ? getDeclaringClassName() : instrumentedClassName;
    }
}
