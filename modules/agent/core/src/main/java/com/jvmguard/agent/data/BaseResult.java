package com.jvmguard.agent.data;

import com.jvmguard.agent.comm.AgentSerializable;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.comm.ProtocolRequirement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class BaseResult implements AgentSerializable {
    protected static File tempDir;

    public static void setTempDir(File tempDir) {
        BaseResult.tempDir = tempDir;
    }

    private long timestamp = System.currentTimeMillis();

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V1;
    }

    public void prepareDeferredDirect(CommunicationContext context) {
    }

    public void prepareDeferredLater(CommunicationContext context) {
    }
}
