package dev.jvmguard.agent.config.base;

public class OptionalConfig extends AbstractEntity {

    @ConfigDoc("Whether this optional configuration section is enabled for the group (if false, the group " +
            "inherits from its parent).")
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
