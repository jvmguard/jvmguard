package dev.jvmguard.agent.parameter;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DataSetParameter extends BaseParameter {
    private long snapshotTimeStamp;

    @DefaultConstructor
    public DataSetParameter() {
    }

    public DataSetParameter(long snapshotTimeStamp) {
        this.snapshotTimeStamp = snapshotTimeStamp;
    }

    public long getSnapshotTimeStamp() {
        return snapshotTimeStamp;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeLong(snapshotTimeStamp);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        snapshotTimeStamp = in.readLong();
    }
}
