package com.jvmguard.agent.config.recording;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.ConfigDoc;
import com.jvmguard.agent.config.base.OptionalConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class RecordingOptions extends OptionalConfig implements AgentSerializable, CodecEntity {

    @ConfigDoc("When configuration changes are applied to classes that are already loaded in the monitored VM.")
    private RetransformationType retransformationType = RetransformationType.ALWAYS;

    public void setRetransformationType(RetransformationType retransformationType) {
        RetransformationType oldValue = this.retransformationType;
        this.retransformationType = retransformationType;
        fireChanged(oldValue, retransformationType);
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
        return "RecordingOptions";
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
        retransformationType = reader.readEnum("retransformationType", RetransformationType.class);
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
        writer.writeEnum("retransformationType", retransformationType);
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V1;
    }
}
