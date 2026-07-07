package com.jvmguard.agent.callee;

import com.jvmguard.agent.config.transactions.ClassFilterTransactionDef;
import com.jvmguard.agent.config.transactions.TransactionType;

@SuppressWarnings("UnusedDeclaration")
public class AnnotationHandler extends Handler {
    private final TransactionType transactionType;

    public AnnotationHandler(ClassFilterTransactionDef policyTransaction, ClassFilterTransactionDef namingTransaction) {
        super(policyTransaction, namingTransaction);
        transactionType = namingTransaction != null ? namingTransaction.getTransactionType() : TransactionType.MAPPED;
    }

    public void enter(int namingId, String staticName, String className, String methodName, Object thisObject) {
        doEnter(transactionType, namingId, staticName, className, methodName, thisObject, null);
    }

}
