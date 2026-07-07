package com.jvmguard.agent.instrument.transaction.matched;

import com.jvmguard.agent.callee.MatchedHandler;
import com.jvmguard.agent.config.transactions.MatchedTransactionDef;
import com.jvmguard.agent.instrument.transaction.TransactionDefList;

public class MatchedTransactionDefList extends TransactionDefList<MatchedTransactionDef, MatchedDefinition, MatchedHandler> {

    public MatchedTransactionDefList(MatchedDefinition definition) {
        super(definition);
    }

    @Override
    protected MatchedHandler createHandler(MatchedTransactionDef policyDef, MatchedTransactionDef namingDef) {
        if (namingDef == null) {
            return null;
        } else {
            return new MatchedHandler(policyDef, namingDef);
        }
    }
}
