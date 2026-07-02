package com.jvmguard.agent.comm;

public enum ProtocolRequirement {
    V1(1),
    V2(2),
    V3(3),
    V4(4),
    V5(5),
    V6(6),
    V7(7),
    V8(8);

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
