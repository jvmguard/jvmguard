package dev.jvmguard.agent.data;

import dev.jvmguard.agent.JvmGuardAgent;
import dev.jvmguard.agent.artifact.ArtifactCache;
import dev.jvmguard.agent.artifact.ArtifactHandler;
import dev.jvmguard.agent.artifact.ArtifactHandlers;
import dev.jvmguard.agent.comm.BlobHelper.BlobResult;
import dev.jvmguard.agent.comm.BlobHelper.TransferAction;
import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.parameter.PushArtifactParameter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class PushArtifactResult extends BaseResult {

    private boolean success;
    private String errorMessage;

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        PushArtifactParameter parameter = (PushArtifactParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
        ArtifactHandler handler = ArtifactHandlers.get(parameter.getKind());
        BlobResult blob = parameter.getBlobResult();

        success = true;
        if (blob.getAction() == TransferAction.ERROR || blob.getFile() == null) {
            success = false;
            errorMessage = blob.getErrorMessage();
        }
        if (success) {
            File archive = blob.getFile();
            try {
                File installDir = handler.getInstallDir(parameter.getKey());
                ArtifactCache.installAtomically(installDir, archive, handler);
                success = handler.isReady(installDir);
                if (!success) {
                    errorMessage = "Artifact not ready after install: " + installDir;
                }
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
                success = false;
                errorMessage = "Error installing artifact: " + t;
            }
            if (blob.getAction() == TransferAction.CONTENT) {
                delete(archive);
            }
        }

        out.writeBoolean(success);
        out.writeUTF(errorMessage != null ? errorMessage : "");
    }

    private void delete(File file) {
        if (file != null && !file.delete()) {
            file.deleteOnExit();
        }
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        success = in.readBoolean();
        errorMessage = in.readUTF();
    }
}
