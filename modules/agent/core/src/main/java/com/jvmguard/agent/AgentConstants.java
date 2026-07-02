package com.jvmguard.agent;

import org.objectweb.asm.Opcodes;

public final class AgentConstants {

    public static final int ASM_VERSION = Opcodes.ASM9;

    public static final String NATIVE_METHOD_PREFIX = "__jvmguard_nmp_";

    public static final int TELEMETRY_TYPE_MEMORY_HEAP_USED = 1;
    public static final int TELEMETRY_TYPE_MEMORY_HEAP_COMMITTED = 2;
    public static final int TELEMETRY_TYPE_MEMORY_NON_HEAP_USED = 4;
    public static final int TELEMETRY_TYPE_MEMORY_NON_HEAP_COMMITTED = 5;
    public static final int TELEMETRY_TYPE_CPU_USAGE = 7;
    public static final int TELEMETRY_TYPE_GC_ACTIVITY = 8;
    public static final int TELEMETRY_TYPE_DEVOPS = 10;
    public static final int TELEMETRY_TYPE_MBEAN = 11;
    public static final int TELEMETRY_TYPE_SYSTEM_LOAD = 13;

    private AgentConstants() {
    }
}
