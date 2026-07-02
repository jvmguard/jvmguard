package com.jvmguard.agent.comm;

public interface CodecEntity {
    String codecType();
    void readState(AgentReader reader) throws Exception;
    void writeState(AgentWriter writer) throws Exception;
}
