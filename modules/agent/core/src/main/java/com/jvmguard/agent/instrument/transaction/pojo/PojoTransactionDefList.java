package com.jvmguard.agent.instrument.transaction.pojo;

import com.jvmguard.agent.callee.PojoHandler;
import com.jvmguard.agent.config.transactions.PojoTransactionDef;
import com.jvmguard.agent.instrument.transaction.TransactionDefList;

public class PojoTransactionDefList extends TransactionDefList<PojoTransactionDef, PojoDefinition, PojoHandler> {

    public PojoTransactionDefList(PojoDefinition definition) {
        super(definition);
    }

    @Override
    protected PojoHandler createHandler(PojoTransactionDef policyDef, PojoTransactionDef namingDef) {
        if (namingDef == null) {
            return null;
        } else {
            return new PojoHandler(policyDef, namingDef);
        }
    }
}
