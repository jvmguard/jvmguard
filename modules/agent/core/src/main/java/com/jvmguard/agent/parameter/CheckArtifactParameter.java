package com.jvmguard.agent.parameter;

import com.jvmguard.agent.artifact.ArtifactKind;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CheckArtifactParameter extends BaseParameter {

    private ArtifactKind kind;
    private String key;

    @DefaultConstructor
    public CheckArtifactParameter() {
    }

    public CheckArtifactParameter(ArtifactKind kind, String key) {
        this.kind = kind;
        this.key = key;
    }

    public ArtifactKind getKind() {
        return kind;
    }

    public String getKey() {
        return key;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeUTF(kind.name());
        out.writeUTF(key);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        kind = ArtifactKind.valueOf(in.readUTF());
        key = in.readUTF();
    }
}
