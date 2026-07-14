package dev.jvmguard.agent.data;

import dev.jvmguard.agent.artifact.ArtifactHandler;
import dev.jvmguard.agent.artifact.ArtifactHandlers;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.parameter.CheckArtifactParameter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class CheckArtifactResult extends BaseResult {

    private boolean available;

    public boolean isAvailable() {
        return available;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        CheckArtifactParameter parameter = (CheckArtifactParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
        ArtifactHandler handler = ArtifactHandlers.get(parameter.getKind());
        File installDir = handler.getInstallDir(parameter.getKey());
        available = installDir.isDirectory() && handler.isReady(installDir);
        out.writeBoolean(available);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        available = in.readBoolean();
    }
}
