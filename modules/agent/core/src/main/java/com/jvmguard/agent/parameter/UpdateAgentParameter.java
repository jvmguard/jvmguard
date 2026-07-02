package com.jvmguard.agent.parameter;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.comm.BlobHelper;
import com.jvmguard.agent.comm.BlobHelper.BlobResult;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class UpdateAgentParameter extends BaseParameter {
    private long buildVersion;
    private File zipFile;
    private BlobResult blobResult;

    @DefaultConstructor
    public UpdateAgentParameter() {
    }

    public UpdateAgentParameter(long buildVersion, File zipFile) {
        this.buildVersion = buildVersion;
        this.zipFile = zipFile;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeLong(buildVersion);
        BlobHelper.writeBlob(context, out, null, zipFile, false);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        buildVersion = in.readLong();
        File agentDir = JvmGuardAgent.getAgentUserDir();
        agentDir.mkdirs();
        blobResult = BlobHelper.readBlob(context, in, agentDir);
    }

    public long getBuildVersion() {
        return buildVersion;
    }

    public BlobResult getBlobResult() {
        return blobResult;
    }
}
