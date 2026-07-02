package com.jvmguard.agent.parameter;

import com.jvmguard.agent.comm.AgentSerializable;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.comm.ProtocolRequirement;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class BaseParameter implements AgentSerializable {
    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
    }

    @Override
    public ProtocolRequirement getProtocolRequirement() {
        return ProtocolRequirement.V1;
    }

}
