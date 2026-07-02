package com.jvmguard.agent.instrument.transaction;

import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.agent.callee.Handler;
import com.jvmguard.agent.config.transactions.ClassFilterTransactionDef;
import com.jvmguard.agent.util.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class TransactionDefList<T extends ClassFilterTransactionDef, D extends TransactionDefinition, H extends Handler> {
    protected List<T> transactionDefs = new ArrayList<>();

    protected final D definition;

    protected abstract H createHandler(T policyDef, T namingDef);

    protected TransactionDefList(D definition) {
        this.definition = definition;
    }

    public void addTransactionDef(T transactionDef) {
        transactionDefs.add(transactionDef);
    }

    public D getDefinition() {
        return definition;
    }

    public H getHandler(DefinitionSite definitionSite) {
        T namingDef = null;
        T policyDef = null;

        Logger.log(Subsystem.INSTRUMENTATION, 6, false, "checking intercepted class %s %s\n", definitionSite, transactionDefs);
        for (int i = 0; i < transactionDefs.size() && (namingDef == null || policyDef == null); i++) {
            T transactionDef = transactionDefs.get(i);
            if (transactionDef.matches(definitionSite)) {
                Logger.log(Subsystem.INSTRUMENTATION, 15, false, "matched %s: %s\n", definitionSite, transactionDef);
                if (namingDef == null && transactionDef.isDiscard()) {
                    Logger.log(Subsystem.INSTRUMENTATION, 5, false, "intercepted class discarded %s\n", definitionSite.getDefinedFor());
                    return null;
                }
                if (policyDef == null && transactionDef.isPolicyActive()) {
                    policyDef = transactionDef;
                }
                if (namingDef == null && transactionDef.isNamingActive()) {
                    namingDef = transactionDef;
                }
            } else {
                Logger.log(Subsystem.INSTRUMENTATION, 15, false, "not matched %s: %s\n", definitionSite, transactionDef);
            }
        }

        if (namingDef != null) {
            Logger.log(Subsystem.INSTRUMENTATION, 5, false, "found %s: (%s, %s)\n", definitionSite.getDefinedFor(), policyDef, namingDef);
        }
        return createHandler(policyDef, namingDef);
    }

    @Override
    public String toString() {
        return "TransactionDefList{" +
            "transactionDefs=" + transactionDefs +
            ", definition=" + definition +
            '}';
    }
}
