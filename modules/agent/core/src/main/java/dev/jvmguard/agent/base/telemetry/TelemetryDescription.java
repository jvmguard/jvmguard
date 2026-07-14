package dev.jvmguard.agent.base.telemetry;

public class TelemetryDescription {
    String name;
    int type;

    public TelemetryDescription(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

}
