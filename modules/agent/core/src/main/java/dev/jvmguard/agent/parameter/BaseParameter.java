package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.comm.AgentSerializable;
import dev.jvmguard.agent.comm.CommunicationContext;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class BaseParameter implements AgentSerializable {
    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
    }

}
