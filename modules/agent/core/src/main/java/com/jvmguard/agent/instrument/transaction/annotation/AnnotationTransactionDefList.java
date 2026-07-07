package com.jvmguard.agent.instrument.transaction.annotation;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.callee.AnnotationHandler;
import com.jvmguard.agent.callee.DeclaredHandler;
import com.jvmguard.agent.config.transactions.AnnotatedTransactionDef;
import com.jvmguard.agent.instrument.transaction.DefinitionSite;
import com.jvmguard.agent.instrument.transaction.TransactionDefList;
import com.jvmguard.agent.util.Logger;

public class AnnotationTransactionDefList extends TransactionDefList<AnnotatedTransactionDef, AnnotationDefinition, AnnotationHandler> {
    public static AnnotationTransactionDefList create(AnnotationDefinition annotationDefinition) {
        if (annotationDefinition instanceof DeclaredAnnotationDefinition) {
            return new DeclaredTransactionDefList(annotationDefinition);
        } else {
            return new AnnotationTransactionDefList(annotationDefinition);
        }
    }

    public AnnotationTransactionDefList(AnnotationDefinition definition) {
        super(definition);
    }

    @Override
    protected AnnotationHandler createHandler(AnnotatedTransactionDef policyDef, AnnotatedTransactionDef namingDef) {
        if (namingDef == null) {
            return null;
        } else {
            return new AnnotationHandler(policyDef, namingDef);
        }
    }

    private static class DeclaredTransactionDefList extends AnnotationTransactionDefList {
        private DeclaredTransactionDefList(AnnotationDefinition definition) {
            super(definition);
        }

        @Override
        public AnnotationHandler getHandler(DefinitionSite definitionSite) {
            Logger.log(Subsystem.INSTRUMENTATION, 6, false, "checking declared intercepted class %s %s\n", definitionSite, transactionDefs);
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < transactionDefs.size(); i++) {
                AnnotatedTransactionDef transactionDef = transactionDefs.get(i);
                if (transactionDef.matches(definitionSite)) {
                    Logger.log(Subsystem.INSTRUMENTATION, 15, false, "matched %s: %s\n", definitionSite, transactionDef);
                    if (transactionDef.isDiscard()) {
                        Logger.log(Subsystem.INSTRUMENTATION, 5, false, "intercepted class discarded %s\n", definitionSite.getDefinedFor());
                        return null;
                    } else if (transactionDef.isPolicyActive()) {
                        Logger.log(Subsystem.INSTRUMENTATION, 5, false, "found %s: (%s)\n", definitionSite.getDefinedFor(), transactionDef);
                        return new DeclaredHandler(transactionDef);
                    }
                } else {
                    Logger.log(Subsystem.INSTRUMENTATION, 15, false, "not matched %s: %s\n", definitionSite, transactionDef);
                }
            }
            return null;
        }
    }


}
