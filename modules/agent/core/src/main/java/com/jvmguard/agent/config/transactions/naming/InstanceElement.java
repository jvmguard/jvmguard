package com.jvmguard.agent.config.transactions.naming;

import com.jvmguard.agent.config.base.DefaultConstructor;
import com.jvmguard.agent.config.transactions.EnvironmentException;

public class InstanceElement extends AbstractGetterElement {

    @DefaultConstructor
    public InstanceElement() {
    }

    public InstanceElement(String getterChain) {
        super(getterChain);
    }

    @Override
    public String codecType() {
        return "InstanceElement";
    }

    @Override
    public String getDisplayName() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Instance");
        appendGetterChain(buffer);
        return buffer.toString();
    }

    public void appendName(StringBuilder buffer, TransactionEnvironment environment) throws EnvironmentException {
        appendName(buffer, environment.getInstance());
    }

    public interface TransactionEnvironment extends InstanceClassNameElement.TransactionEnvironment {
        Object getInstance();
    }

}
