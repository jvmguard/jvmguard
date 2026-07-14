package dev.jvmguard.agent.config.transactions;

// do not rename enums
public enum FrequencyUnit {
    REQUESTS("Requests"),
    MINUTES("Minutes"),
    HOURS("Hours");

    private final String verbose;

    FrequencyUnit(String verbose) {
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        return verbose;
    }
}
