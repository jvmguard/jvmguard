package com.jvmguard.agent.callee;

import com.jvmguard.agent.config.transactions.ClassFilterTransactionDef;
import com.jvmguard.agent.config.transactions.TransactionType;

public class MatchedHandler extends Handler {

    public MatchedHandler(ClassFilterTransactionDef policyTransaction, ClassFilterTransactionDef namingTransaction) {
        super(policyTransaction, namingTransaction);
    }

    public void enter(int namingId, String staticName, String className, String methodName, Object thisObject, Object[] parameter) {
        doEnter(TransactionType.MATCHED, namingId, staticName, className, methodName, thisObject, parameter);
    }
}
