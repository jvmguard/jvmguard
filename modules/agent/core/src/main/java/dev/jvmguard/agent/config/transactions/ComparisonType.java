package dev.jvmguard.agent.config.transactions;

import dev.jvmguard.agent.config.base.ConfigDoc;

public enum ComparisonType {
    @ConfigDoc("Match using wildcard patterns.")
    WILDCARD("Wildcard comparison"),
    @ConfigDoc("Match using a regular expression.")
    REGEX("Regular expression");

    private final String verbose;

    ComparisonType(String verbose) {
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        return verbose;
    }
}
