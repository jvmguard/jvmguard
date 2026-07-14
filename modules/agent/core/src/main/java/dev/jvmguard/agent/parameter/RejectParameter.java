package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RejectParameter extends BaseParameter {
    private String errorMessage;

    @DefaultConstructor
    public RejectParameter() {
    }

    public RejectParameter(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeUTF(errorMessage);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        errorMessage = in.readUTF();
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
