package com.jvmguard.agent.config.base;

public enum LogCategory {
    @ConfigDoc("Informational severity.")
    INFO("Info"),
    @ConfigDoc("Warning severity.")
    WARNING("Warning"),
    @ConfigDoc("Error severity.")
    ERROR("Error");

    private final String verbose;

    LogCategory(String verbose) {
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        return verbose;
    }

}
