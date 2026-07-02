package com.jvmguard.agent.config;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.recording.RecordingOptions;
import com.jvmguard.agent.config.telemetry.TelemetrySettings;
import com.jvmguard.agent.config.transactions.TransactionSettings;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class AgentGroupConfig implements AgentSerializable, CodecEntity {

    private TransactionSettings transactionSettings = new TransactionSettings();
    private RecordingOptions recordingOptions = new RecordingOptions();
    private TelemetrySettings telemetrySettings = new TelemetrySettings();

    public TransactionSettings getTransactionSettings() {
        return transactionSettings;
    }

    public void setTransactionSettings(TransactionSettings transactionSettings) {
        this.transactionSettings = transactionSettings;
    }

    public RecordingOptions getRecordingOptions() {
        return recordingOptions;
    }

    public void setRecordingOptions(RecordingOptions recordingOptions) {
        this.recordingOptions = recordingOptions;
    }

    public TelemetrySettings getTelemetrySettings() {
        return telemetrySettings;
    }

    public void setTelemetrySettings(TelemetrySettings telemetrySettings) {
        this.telemetrySettings = telemetrySettings;
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
        return "AgentGroupConfig";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        recordingOptions = reader.readObject("recordingOptions");
        transactionSettings = reader.readObject("transactionSettings");
        telemetrySettings = reader.readObject("telemetrySettings");
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeObject("recordingOptions", recordingOptions);
        writer.writeObject("transactionSettings", transactionSettings);
        writer.writeObject("telemetrySettings", telemetrySettings);
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V1;
    }

    @Override
    public String toString() {
        return "AgentGroupConfig{" +
            "transactionSettings=" + transactionSettings +
            ", recordingOptions=" + recordingOptions +
            ", telemetrySettings=" + telemetrySettings +
            '}';
    }
}
