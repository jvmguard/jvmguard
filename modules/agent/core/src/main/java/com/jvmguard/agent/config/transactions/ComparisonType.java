package com.jvmguard.agent.config.transactions;

public enum ComparisonType {
    WILDCARD("Wildcard comparison"),
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
