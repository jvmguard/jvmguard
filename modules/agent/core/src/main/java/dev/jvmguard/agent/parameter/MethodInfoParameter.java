package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MethodInfoParameter extends BaseParameter {
    private String className;

    @DefaultConstructor
    public MethodInfoParameter() {
    }

    public MethodInfoParameter(String className) {
        this.className = className;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeUTF(className);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        className = in.readUTF();
    }

    public String getClassName() {
        return className;
    }
}
