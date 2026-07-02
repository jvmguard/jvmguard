package com.jvmguard.agent.config.transactions;

public enum ReentryInhibition {
    NAME("With the same name"),
    DEF("That match this entry"),
    GROUP("With the same group name"),
    TYPE("Of the same transaction type"),
    ALL("All further entries");

    private final String verbose;

    ReentryInhibition(String verbose) {
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        return verbose;
    }
}
