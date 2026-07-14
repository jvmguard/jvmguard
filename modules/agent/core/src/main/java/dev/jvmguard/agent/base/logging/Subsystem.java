package dev.jvmguard.agent.base.logging;

public enum Subsystem {
    COMMON("cmn> ", "Common"),
    INSTRUMENTATION("inst> ", "Instrumentation"),
    COMMUNICATION("comm> ", "Communication"),
    USER("usr> ", "User"),
    MBEAN("mbean> ", "MBean");

    private final String prefix;
    private final String propertySuffix;

    Subsystem(String prefix, String propertySuffix) {
        this.prefix = prefix;
        this.propertySuffix = propertySuffix;
    }

    public String getPropertySuffix() {
        return propertySuffix;
    }

    @Override
    public String toString() {
        return prefix;
    }
}
