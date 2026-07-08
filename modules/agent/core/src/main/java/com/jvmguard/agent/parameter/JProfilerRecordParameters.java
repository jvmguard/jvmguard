package com.jvmguard.agent.parameter;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JProfilerRecordParameters extends BaseParameter {

    private int seconds;
    private String artifactKey;

    @DefaultConstructor
    public JProfilerRecordParameters() {
    }

    public JProfilerRecordParameters(int seconds, String artifactKey) {
        this.seconds = seconds;
        this.artifactKey = artifactKey;
    }

    public int getSeconds() {
        return seconds;
    }

    public String getArtifactKey() {
        return artifactKey;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeInt(seconds);
        out.writeUTF(artifactKey);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        seconds = in.readInt();
        artifactKey = in.readUTF();
    }
}
