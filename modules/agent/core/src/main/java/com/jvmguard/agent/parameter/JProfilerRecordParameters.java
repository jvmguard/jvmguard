package com.jvmguard.agent.parameter;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JProfilerRecordParameters extends BaseParameter {

    private int seconds;
    private String artifactKey;
    private String[] subsystems = new String[0];
    private boolean heapDump;
    private boolean heapDumpFullGc = true;
    private boolean mbeanSnapshot;
    private boolean monitorDump;

    @DefaultConstructor
    public JProfilerRecordParameters() {
    }

    public JProfilerRecordParameters(int seconds, String artifactKey, String[] subsystems,
                                     boolean heapDump, boolean heapDumpFullGc,
                                     boolean mbeanSnapshot, boolean monitorDump) {
        this.seconds = seconds;
        this.artifactKey = artifactKey;
        this.subsystems = subsystems;
        this.heapDump = heapDump;
        this.heapDumpFullGc = heapDumpFullGc;
        this.mbeanSnapshot = mbeanSnapshot;
        this.monitorDump = monitorDump;
    }

    public int getSeconds() {
        return seconds;
    }

    public String getArtifactKey() {
        return artifactKey;
    }

    public String[] getSubsystems() {
        return subsystems;
    }

    public boolean isHeapDump() {
        return heapDump;
    }

    public boolean isHeapDumpFullGc() {
        return heapDumpFullGc;
    }

    public boolean isMbeanSnapshot() {
        return mbeanSnapshot;
    }

    public boolean isMonitorDump() {
        return monitorDump;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeInt(seconds);
        out.writeUTF(artifactKey);
        out.writeInt(subsystems.length);
        for (String subsystem : subsystems) {
            out.writeUTF(subsystem);
        }
        out.writeBoolean(heapDump);
        out.writeBoolean(heapDumpFullGc);
        out.writeBoolean(mbeanSnapshot);
        out.writeBoolean(monitorDump);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        seconds = in.readInt();
        artifactKey = in.readUTF();
        subsystems = new String[in.readInt()];
        for (int i = 0; i < subsystems.length; i++) {
            subsystems[i] = in.readUTF();
        }
        heapDump = in.readBoolean();
        heapDumpFullGc = in.readBoolean();
        mbeanSnapshot = in.readBoolean();
        monitorDump = in.readBoolean();
    }
}
