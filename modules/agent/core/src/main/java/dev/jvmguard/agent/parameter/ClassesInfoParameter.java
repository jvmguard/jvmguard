package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClassesInfoParameter extends BaseParameter {
    private boolean allClasses;

    @DefaultConstructor
    public ClassesInfoParameter() {
    }

    public ClassesInfoParameter(boolean allClasses) {
        this.allClasses = allClasses;
    }

    public boolean isAllClasses() {
        return allClasses;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeBoolean(allClasses);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        allClasses = in.readBoolean();
    }

}
