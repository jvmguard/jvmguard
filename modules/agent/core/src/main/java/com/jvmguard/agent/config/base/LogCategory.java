package com.jvmguard.agent.config.base;

public enum LogCategory {
    INFO("Info"),
    WARNING("Warning"),
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
