package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;

public abstract class AnnotatedTransactionDef extends ClassFilterTransactionDef {
    public abstract AnnotationDefinition[] getAnnotationDefinitions();
}
