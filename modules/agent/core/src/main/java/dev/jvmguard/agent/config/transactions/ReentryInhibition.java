package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.config.base.ConfigDoc;

public enum ReentryInhibition {
    @ConfigDoc("Suppress re-entrant entries with the same name.")
    NAME("With the same name"),
    @ConfigDoc("Suppress entries matching this same definition.")
    DEF("That match this entry"),
    @ConfigDoc("Suppress entries with the same group name.")
    GROUP("With the same group name"),
    @ConfigDoc("Suppress entries of the same transaction type.")
    TYPE("Of the same transaction type"),
    @ConfigDoc("Suppress all further nested entries.")
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
