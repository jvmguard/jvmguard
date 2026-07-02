package com.jvmguard.agent.parameter;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AvailableAgentParameter extends BaseParameter {
    private long buildVersion;

    @DefaultConstructor
    public AvailableAgentParameter() {
    }

    public AvailableAgentParameter(long buildVersion) {
        this.buildVersion = buildVersion;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeLong(buildVersion);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        buildVersion = in.readLong();
    }

    public long getBuildVersion() {
        return buildVersion;
    }
}
