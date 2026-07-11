package com.jvmguard.agent.config.recording;

import com.jvmguard.agent.config.base.ConfigDoc;

public enum RetransformationType {
    @ConfigDoc("Retransform classes on all configuration changes.")
    ALWAYS("Retransform classes on all configuration changes"),
    @ConfigDoc("Only retransform the first time that a VM connects to the server.")
    FIRST_CONNECTION("Only retransform the first time that a VM connects to the server"),
    @ConfigDoc("No retransformation. Uses the previous configuration at startup.")
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
