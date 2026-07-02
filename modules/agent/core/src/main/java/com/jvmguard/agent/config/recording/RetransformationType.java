package com.jvmguard.agent.config.recording;

public enum RetransformationType {
    ALWAYS("Retransform classes on all configuration changes"),
    FIRST_CONNECTION("Only retransform the first time that a VM connects to the server"),
    STARTUP("No retransformation, use the previous configuration at startup");

    private final String description;

    RetransformationType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
