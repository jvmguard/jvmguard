package dev.jvmguard.agent.instrument.interceptions;

import dev.jvmguard.agent.instrument.model.InterceptionMethod;
import dev.jvmguard.agent.callee.Handler;
import dev.jvmguard.agent.instrument.transaction.TransactionDefinition;
import dev.jvmguard.agent.instrument.transaction.annotation.MappedAnnotationDefinition;

import java.util.Set;

public class AnnotationInterception extends TransactionInterception {

    protected final String declaringClassName;

    public AnnotationInterception(TransactionDefinition definition, Set<InterceptionMethod> noTransactionMethods, Handler handler, String declaringClassName) {
        super(definition, noTransactionMethods, handler);
        this.declaringClassName = declaringClassName.replace('/', '.');
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
