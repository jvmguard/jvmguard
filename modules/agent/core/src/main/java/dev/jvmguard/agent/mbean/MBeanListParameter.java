package dev.jvmguard.agent.mbean;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.DefaultConstructor;
import dev.jvmguard.agent.parameter.BaseParameter;

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
}
