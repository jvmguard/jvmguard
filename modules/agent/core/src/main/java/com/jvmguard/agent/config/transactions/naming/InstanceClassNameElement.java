package com.jvmguard.agent.config.transactions.naming;

import com.jvmguard.agent.config.base.DefaultConstructor;

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
