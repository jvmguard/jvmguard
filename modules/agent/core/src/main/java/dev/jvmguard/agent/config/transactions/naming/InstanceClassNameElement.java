package dev.jvmguard.agent.config.transactions.naming;

import dev.jvmguard.agent.config.base.ConfigDoc;
import dev.jvmguard.agent.config.base.DefaultConstructor;

@ConfigDoc("Adds the runtime instance's class name (optionally with package) as a name segment.")
public class InstanceClassNameElement extends ClassNameElement {

    @DefaultConstructor
    public InstanceClassNameElement() {
    }

    public InstanceClassNameElement(PackageMode packageMode) {
        super(packageMode);
    }

    @Override
    public String codecType() {
        return "InstanceClassNameElement";
    }

    @Override
    public String getDisplayName() {
        return "Instance class name " + getPackageMode().toString().toLowerCase();
    }

    @Override
    public boolean canBeStatic() {
        return false;
    }

    @Override
    protected String getClassName(ClassNameElement.TransactionEnvironment environment) {
        return ((TransactionEnvironment)environment).getInstanceClassName();
    }

    public interface TransactionEnvironment extends ClassNameElement.TransactionEnvironment {
        String getInstanceClassName();
    }
}
