package com.jvmguard.agent.parameter;

import com.jvmguard.agent.artifact.ArtifactHandlers;
import com.jvmguard.agent.artifact.ArtifactKind;
import com.jvmguard.agent.comm.BlobHelper;
import com.jvmguard.agent.comm.BlobHelper.BlobResult;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class PushArtifactParameter extends BaseParameter {

    private ArtifactKind kind;
    private String key;
    private File archiveFile;      // populated on the sending (server) side
    private BlobResult blobResult; // populated on the receiving (agent) side

    @DefaultConstructor
    public PushArtifactParameter() {
    }

    public PushArtifactParameter(ArtifactKind kind, String key, File archiveFile) {
        this.kind = kind;
        this.key = key;
        this.archiveFile = archiveFile;
    }

    public ArtifactKind getKind() {
        return kind;
    }

    public String getKey() {
        return key;
    }

    public BlobResult getBlobResult() {
        return blobResult;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeUTF(kind.name());
        out.writeUTF(key);
        BlobHelper.writeBlob(context, out, null, archiveFile, false);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        kind = ArtifactKind.valueOf(in.readUTF());
        key = in.readUTF();
        File cacheBaseDir = ArtifactHandlers.get(kind).getCacheBaseDir();
        cacheBaseDir.mkdirs();
        blobResult = BlobHelper.readBlob(context, in, cacheBaseDir);
    }
}
