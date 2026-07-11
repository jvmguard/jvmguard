package com.jvmguard.agent.config.transactions.naming;

import com.jvmguard.agent.comm.*;
import com.jvmguard.agent.config.base.ConfigDoc;
import com.jvmguard.agent.config.transactions.NamingElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;

@ConfigDoc("Adds the intercepted method's name as a name segment.")
public class MethodNameElement extends NamingElement {

    @Override
    public String codecType() {
        return "MethodNameElement";
    }

    @Override
    public String getDisplayName() {
        return "Method name";
    }

    @Override
    public boolean canBeStatic() {
        return true;
    }

    public void appendName(StringBuilder buffer, TransactionEnvironment environment) {
        buffer.append(environment.getMethodName());
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws Exception {
        readState(new BinaryAgentReader(in));
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws Exception {
        writeState(new BinaryAgentWriter(out));
    }

    @Override
    public void readState(AgentReader reader) throws Exception {
    }

    @Override
    public void writeState(AgentWriter writer) throws Exception {
    }

    public interface TransactionEnvironment {
        String getMethodName();
    }
}
