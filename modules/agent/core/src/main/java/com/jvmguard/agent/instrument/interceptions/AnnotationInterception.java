package com.jvmguard.agent.instrument.interceptions;

import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.agent.callee.Handler;
import com.jvmguard.agent.instrument.transaction.TransactionDefinition;
import com.jvmguard.agent.instrument.transaction.annotation.CustomAnnotationDefinition;

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
        boolean declaringClassName = getDefinition() instanceof CustomAnnotationDefinition ? ((CustomAnnotationDefinition)getDefinition()).isUseDeclaringClassName() : false;
        return declaringClassName ? getDeclaringClassName() : instrumentedClassName;
    }
}
