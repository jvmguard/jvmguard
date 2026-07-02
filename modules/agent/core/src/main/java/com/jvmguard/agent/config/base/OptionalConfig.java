package com.jvmguard.agent.config.base;

public class OptionalConfig extends AbstractEntity {

    private boolean used = false;

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        boolean oldValue = this.used;
        this.used = used;
        fireChanged(oldValue, used);
    }
}
