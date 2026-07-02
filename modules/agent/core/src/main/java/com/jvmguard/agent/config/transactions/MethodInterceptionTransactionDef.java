package com.jvmguard.agent.config.transactions;

import com.jvmguard.agent.config.transactions.naming.MethodNameElement;

import java.util.List;

public abstract class MethodInterceptionTransactionDef extends ClassFilterTransactionDef {

    public MethodInterceptionTransactionDef() {
    }

    public static String getTransactionName(List<NamingElement> namingElements, MethodInterceptionTransactionEnvironment environment) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < namingElements.size(); i++) {
            NamingElement namingElement = namingElements.get(i);
            try {
                if (namingElement instanceof MethodNameElement) {
                    ((MethodNameElement)namingElement).appendName(buffer, environment);
                } else {
                    classInterceptionAppendTransactionName(namingElement, environment, buffer);
                }
            } catch (EnvironmentException e) {
                handleEnvironmentException(e, environment, i + 1);
            }
        }
        return buffer.toString();
    }

    public static class MethodInterceptionTransactionEnvironment extends ClassInterceptionTransactionEnvironment implements
        MethodNameElement.TransactionEnvironment {

        private String methodName;

        public MethodInterceptionTransactionEnvironment(String className, String methodName, Object thisObject, Object[] parameterObjects) {
            super(className, thisObject, parameterObjects);
            this.methodName = methodName;
        }

        @Override
        public String getMethodName() {
            return methodName;
        }

        @Override
        public String toString() {
            return super.toString() + "." + getMethodName();
        }
    }
}
