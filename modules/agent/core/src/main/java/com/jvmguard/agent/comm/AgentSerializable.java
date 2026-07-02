package com.jvmguard.agent.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Serializable;

public interface AgentSerializable extends Serializable {
    void read(CommunicationContext context, DataInputStream in) throws Exception;
    void write(CommunicationContext context, DataOutputStream out) throws Exception;
    ProtocolRequirement getProtocolRequirement();
}
