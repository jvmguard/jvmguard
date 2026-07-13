package com.jvmguard.agent.comm;

public enum ProtocolRequirement {
    V1(1);

    private final int version;

    ProtocolRequirement(int version) {
        this.version = version;
    }

    public boolean satisfies(int version) {
        return version >= this.version;
    }

    int getVersion() {
        return version;
    }
}
