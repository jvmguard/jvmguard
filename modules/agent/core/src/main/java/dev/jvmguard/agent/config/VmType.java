package dev.jvmguard.agent.config;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public enum VmType {

    GROUP("VM Group", true, 1),
    POOL("VM Pool", true, 2),
    NAMED("Named VM", false, 3),
    POOLED("Pooled VM", false, 4); // retain order to provide a single restriction for most SQL queries

    private final String verbose;
    private final boolean groupNode;
    private final int databaseId;

    private static final Int2ObjectOpenHashMap<VmType> databaseIdToType = new Int2ObjectOpenHashMap<>();

    static {
        for (VmType vmType : values()) {
            databaseIdToType.put(vmType.getDatabaseId(), vmType);
        }
    }

    VmType(String verbose, boolean groupNode, int databaseId) {
        this.verbose = verbose;
        this.groupNode = groupNode;
        this.databaseId = databaseId;
    }

    public boolean isGroupNode() {
        return groupNode;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public static VmType fromDatabaseId(int id) {
        return databaseIdToType.get(id);
    }

    public VmType getParentType() {
        return this == POOLED ? POOL : GROUP;
    }

    @Override
    public String toString() {
        return verbose;
    }
}
