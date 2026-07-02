package com.jvmguard.agent.instrument.classInfo;

import com.jvmguard.agent.instrument.model.InterceptionMethod;
import com.jvmguard.annotation.ClassTransaction;
import com.jvmguard.annotation.MethodTransaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DevOpsAnnotations {
    private Map<InterceptionMethod, MethodTransaction> methodTransactions = new HashMap<>();
    private ClassTransaction classTransaction;

    private boolean noTransaction;
    private Set<InterceptionMethod> noTransactionMethods = new HashSet<>();

    public void setNoTransaction(boolean noTransaction) {
        this.noTransaction = noTransaction;
    }

    public void addNoTransactionMethod(InterceptionMethod interceptionMethod) {
        noTransactionMethods.add(interceptionMethod);
    }

    public void addMethodTransaction(InterceptionMethod interceptionMethod, MethodTransaction methodTransaction) {
        methodTransactions.put(interceptionMethod, methodTransaction);
    }

    public void setClassTransaction(ClassTransaction classTransaction) {
        this.classTransaction = classTransaction;
    }

    public Map<InterceptionMethod, MethodTransaction> getMethodTransactions() {
        return methodTransactions;
    }

    public ClassTransaction getClassTransaction() {
        return classTransaction;
    }

    public boolean isNoTransaction() {
        return noTransaction;
    }

    public Set<InterceptionMethod> getNoTransactionMethods() {
        return noTransactionMethods;
    }

    @Override
    public String toString() {
        return "DevOpsAnnotations{" +
            "methodTransactions=" + methodTransactions +
            ", classTransaction=" + classTransaction +
            ", noTransaction=" + noTransaction +
            ", noTransactionMethods=" + noTransactionMethods +
            '}';
    }
}
