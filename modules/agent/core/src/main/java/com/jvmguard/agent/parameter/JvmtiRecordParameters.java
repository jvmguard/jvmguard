package com.jvmguard.agent.parameter;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JvmtiRecordParameters extends BaseParameter {

    private int seconds;
    private boolean splitTransactions;
    private boolean sampleAgentClasses;
    private boolean sampleAgentThreads;

    @DefaultConstructor
    public JvmtiRecordParameters() {
    }

    public JvmtiRecordParameters(int seconds, boolean splitTransactions) {
        this.seconds = seconds;
        this.splitTransactions = splitTransactions;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        seconds = in.readInt();
        splitTransactions = in.readBoolean();
        sampleAgentClasses = in.readBoolean();
        sampleAgentThreads = in.readBoolean();
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeInt(seconds);
        out.writeBoolean(splitTransactions);
        out.writeBoolean(sampleAgentClasses);
        out.writeBoolean(sampleAgentThreads);
    }

    @Override
    public String toString() {
        return "JvmtiRecordParameters{" +
            "time=" + seconds +
            '}';
    }
}
