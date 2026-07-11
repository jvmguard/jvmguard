package com.jvmguard.agent.config.telemetry;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.ConfigDoc;
import com.jvmguard.agent.config.base.OptionalConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class TelemetrySettings extends OptionalConfig implements AgentSerializable, CodecEntity {

    @ConfigDoc("Custom MBean-based telemetry charts to collect from monitored JVMs.")
    private List<MBeanTelemetryConfig> mbeanTelemetries = new ArrayList<>();

    public List<MBeanTelemetryConfig> getMbeanTelemetries() {
        return mbeanTelemetries;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
        readState(new BinaryAgentReader(in));
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
        writeState(new BinaryAgentWriter(out));
    }

    @Override
    public String codecType() {
        return "TelemetrySettings";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        reader.readList("mbeanTelemetries", mbeanTelemetries);
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeList("mbeanTelemetries", mbeanTelemetries);
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V2;
    }
}
