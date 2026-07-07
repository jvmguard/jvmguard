package com.jvmguard.agent.config.transactions;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public enum TransactionType {
    MATCHED("Matched invocation", 2),
    MAPPED("Mapped invocation", 3),
    DECLARED("Declared invocation", 4),
    VM("VM", 8),
    CALL_WITHOUT_TRANSACTION("Call without transaction", 13);

    private final String transactionName;
    private final short id;

    private static final Int2ObjectOpenHashMap<TransactionType> idToType = new Int2ObjectOpenHashMap<>();

    static {
        for (TransactionType transactionType : TransactionType.values()) {
            idToType.put(transactionType.getId(), transactionType);
        }
    }

    TransactionType(String transactionName, int id) {
        this.transactionName = transactionName;
        this.id = (short)id;
    }

    public short getId() {
        return id;
    }

    @Override
    public String toString() {
        return transactionName;
    }

    public static TransactionType fromId(int id) {
        return idToType.get(id);
    }
}
