package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.instrument.transaction.annotation.AnnotationDefinition;

public abstract class AnnotatedTransactionDef extends ClassFilterTransactionDef {
    public abstract AnnotationDefinition[] getAnnotationDefinitions();
}
