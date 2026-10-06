package dev.jvmguard.agent.config;

import dev.jvmguard.agent.comm.*;
import dev.jvmguard.agent.config.recording.RetransformationType;
import dev.jvmguard.agent.config.telemetry.TelemetrySettings;
import dev.jvmguard.agent.config.transactions.TransactionSettings;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class AgentGroupConfig implements AgentSerializable, CodecEntity {

    private static final CodecEntity LEGACY_RECORDING_OPTIONS = new LegacyRecordingOptions();

    private TransactionSettings transactionSettings = new TransactionSettings();
    private TelemetrySettings telemetrySettings = new TelemetrySettings();

    public TransactionSettings getTransactionSettings() {
        return transactionSettings;
    }

    public void setTransactionSettings(TransactionSettings transactionSettings) {
        this.transactionSettings = transactionSettings;
    }

    public TelemetrySettings getTelemetrySettings() {
        return telemetrySettings;
    }

    public void setTelemetrySettings(TelemetrySettings telemetrySettings) {
        this.telemetrySettings = telemetrySettings;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
        readState(new BinaryAgentReader(in, context));
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
        writeState(new BinaryAgentWriter(out, context));
    }

    @Override
    public String codecType() {
        return "AgentGroupConfig";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        if (!reader.satisfies(ProtocolRequirement.V2)) {
            reader.readObject("recordingOptions");
        }
        transactionSettings = reader.readObject("transactionSettings");
        telemetrySettings = reader.readObject("telemetrySettings");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        if (!writer.satisfies(ProtocolRequirement.V2)) {
            writer.writeObject("recordingOptions", LEGACY_RECORDING_OPTIONS);
        }
        writer.writeObject("transactionSettings", transactionSettings);
        writer.writeObject("telemetrySettings", telemetrySettings);
    }

    public static class LegacyRecordingOptions implements CodecEntity {
        @Override
        public String codecType() {
            return "RecordingOptions";
        }

        @Override
        public void readState(AgentReader reader) throws Exception {
            reader.readEnum("retransformationType", RetransformationType.class);
        }

        @Override
        public void writeState(AgentWriter writer) throws Exception {
            writer.writeEnum("retransformationType", RetransformationType.ALWAYS);
        }
    }

    @Override
    public String toString() {
        return "AgentGroupConfig{" +
            "transactionSettings=" + transactionSettings +
            ", telemetrySettings=" + telemetrySettings +
            '}';
    }
}
