package com.jvmguard.agent.callee;

import com.jvmguard.agent.RequestSession;
import com.jvmguard.agent.config.transactions.*;
import com.jvmguard.agent.config.transactions.MethodInterceptionTransactionDef.MethodInterceptionTransactionEnvironment;

public class Handler {

    protected final ClassFilterTransactionDef policyTransaction;
    protected final ClassFilterTransactionDef namingTransaction;

    protected final boolean needsArguments;

    public Handler(ClassFilterTransactionDef policyTransaction, ClassFilterTransactionDef namingTransaction) {
        this.policyTransaction = policyTransaction;
        this.namingTransaction = namingTransaction;
        needsArguments = namingTransaction == null ? false : namingTransaction.needsArguments();
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean needsArguments() {
        return needsArguments;
    }

    public void exit() {
        RequestSession.getInstance().getThreadManager().exitInterceptionMethod(null);
    }

    public void exception(Throwable t) {
        RequestSession.getInstance().getThreadManager().exitInterceptionMethod(t);
    }

    protected String calculateTransactionName(String className, String methodName, Object thisObject, Object[] parameter) {
        MethodInterceptionTransactionEnvironment environment = new MethodInterceptionTransactionEnvironment(className, methodName, thisObject, parameter);
        return MethodInterceptionTransactionDef.getTransactionName(namingTransaction.getNaming().getNamingElements(), environment);
    }

    public String calculateStaticTransactionName(String className, String methodName) {
        return calculateTransactionName(className, methodName, null, null);
    }

    protected void doEnter(TransactionType transactionType, int namingId, String staticName, String className, String methodName, Object thisObject, Object[] parameter) {
        String transactionName = staticName;
        if (transactionName == null || namingId != namingTransaction.getNaming().namingIdentifier()) {
            transactionName = calculateTransactionName(className, methodName, thisObject, parameter);
        }
        PolicyDef policyDef = getPolicyDef(transactionName);
        if (policyDef != TransactionDef.DISCARD_POLICY_DEF) {
            RequestSession.getInstance().getThreadManager().enterInterceptionMethod(transactionName, null, transactionType, policyDef, namingTransaction);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Handler)) {
            return false;
        }

        Handler that = (Handler)o;

        if (namingTransaction != that.namingTransaction) {
            return false;
        }
        if (policyTransaction != that.policyTransaction) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = policyTransaction != null ? policyTransaction.hashCode() : 0;
        result = 31 * result + (namingTransaction != null ? namingTransaction.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Handler{" +
            "policyTransaction=" + policyTransaction +
            ", namingTransaction=" + namingTransaction +
            '}';
    }

    protected PolicyDef getPolicyDef(String transactionName) {
        return policyTransaction == null ? null : policyTransaction.findPolicyDef(transactionName);
    }

    public TransactionNaming getNaming() {
        return namingTransaction == null ? null : namingTransaction.getNaming();
    }

}
