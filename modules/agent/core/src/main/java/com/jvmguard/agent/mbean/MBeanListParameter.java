package com.jvmguard.agent.mbean;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.comm.ProtocolRequirement;
import com.jvmguard.agent.config.base.DefaultConstructor;
import com.jvmguard.agent.parameter.BaseParameter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MBeanListParameter extends BaseParameter {
    private boolean createPlatformServer;

    @DefaultConstructor
    public MBeanListParameter() {
    }

    public MBeanListParameter(boolean createPlatformServer) {
        this.createPlatformServer = createPlatformServer;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeBoolean(createPlatformServer);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        createPlatformServer = in.readBoolean();
    }

    public boolean isCreatePlatformServer() {
        return createPlatformServer;
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V2;
    }
}
