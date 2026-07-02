package com.jvmguard.agent.data;

import com.jvmguard.agent.AgentInit;
import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.comm.BlobHelper.TransferAction;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.parameter.UpdateAgentParameter;
import com.jvmguard.agent.util.JvmGuardUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class UpdateAgentResult extends BaseResult {

    private String errorMessage;
    private boolean success;

    @SuppressWarnings("UnusedDeclaration")
    public UpdateAgentResult() {
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        UpdateAgentParameter parameter = (UpdateAgentParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);

        success = true;
        if (parameter.getBlobResult().getAction() == TransferAction.ERROR || parameter.getBlobResult().getFile() == null) {
            success = false;
            errorMessage = parameter.getBlobResult().getErrorMessage();
        }
        if (success) {
            File zipFile = parameter.getBlobResult().getFile();
            try {
                File agentDir = new File(JvmGuardAgent.getAgentUserDir(), String.valueOf(parameter.getBuildVersion()));
                if (!agentDir.exists()) {
                    File dirTempFile = File.createTempFile("agent", ".tmp", JvmGuardAgent.getAgentUserDir());
                    File dir = new File(dirTempFile.getParentFile(), dirTempFile.getName() + ".dir");
                    dir.mkdirs();
                    if (dir.isDirectory()) {
                        JvmGuardUtil.unpack(zipFile, dir);
                    }
                    if (!dir.renameTo(agentDir)) {
                        JvmGuardUtil.deleteDirectory(dir);
                    }
                    delete(dirTempFile);
                }
                File javaAgentJar = new File(agentDir, AgentInit.AGENT_JAR);
                success = javaAgentJar.isFile();
                if (!success) {
                    errorMessage = "Error unpacking agent: " + javaAgentJar + " not present.";
                }
            } catch (Throwable t) {
                JvmGuardAgent.log(t);
                success = false;
                errorMessage = "Error unpacking agent: " + t;
            }
            if (parameter.getBlobResult().getAction() == TransferAction.CONTENT) {
                delete(zipFile);
            }
        }

        out.writeBoolean(success);
        out.writeUTF(errorMessage != null ? errorMessage : "");
    }

    private void delete(File dirTempFile) {
        if (!dirTempFile.delete()) {
            dirTempFile.deleteOnExit();
        }
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        success = in.readBoolean();
        errorMessage = in.readUTF();
    }
}
