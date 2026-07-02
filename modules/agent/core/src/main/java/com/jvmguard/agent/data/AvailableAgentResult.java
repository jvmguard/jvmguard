package com.jvmguard.agent.data;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.parameter.AvailableAgentParameter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class AvailableAgentResult extends BaseResult {

    private boolean available;
    private String osName;
    private String osArch;
    private String osVersion;

    public AvailableAgentResult() {
    }

    public boolean isAvailable() {
        return available;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsArch() {
        return osArch;
    }

    public String getOsVersion() {
        return osVersion;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        AvailableAgentParameter parameter = (AvailableAgentParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
        boolean available = new File(JvmGuardAgent.getAgentUserDir(), String.valueOf(parameter.getBuildVersion())).isDirectory();
        out.writeBoolean(available);
        out.writeUTF(System.getProperty("os.name"));
        out.writeUTF(System.getProperty("os.arch"));
        out.writeUTF(System.getProperty("os.version"));

    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        available = in.readBoolean();
        osName = in.readUTF();
        osArch = in.readUTF();
        osVersion = in.readUTF();
    }
}
