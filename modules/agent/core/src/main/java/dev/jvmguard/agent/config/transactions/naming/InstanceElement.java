package dev.jvmguard.agent.config.transactions.naming;

import dev.jvmguard.agent.config.base.ConfigDoc;
import dev.jvmguard.agent.config.base.DefaultConstructor;
import dev.jvmguard.agent.config.transactions.EnvironmentException;

@ConfigDoc("Adds a value obtained by a getter/field chain on the intercepted instance.")
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
