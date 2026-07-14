package dev.jvmguard.agent.mbean;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.DefaultConstructor;
import dev.jvmguard.agent.parameter.BaseParameter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MBeanParameter extends BaseParameter {
    private String name;

    @DefaultConstructor
    public MBeanParameter() {
    }

    public MBeanParameter(String name) {
        this.name = name;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeUTF(name);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        name = in.readUTF();
    }

    public String getName() {
        return name;
    }
}
