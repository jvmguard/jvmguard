package com.jvmguard.agent.parameter;

import com.jvmguard.agent.RequestSession;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ResetParameter extends BaseParameter {
    private long lastSnapshotTimestamp;

    @DefaultConstructor
    public ResetParameter() {
    }

    public ResetParameter(long lastSnapshotTimestamp) {
        this.lastSnapshotTimestamp = lastSnapshotTimestamp;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeLong(lastSnapshotTimestamp);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        lastSnapshotTimestamp = in.readLong();
        RequestSession.getInstance().reset(lastSnapshotTimestamp);
    }
}
