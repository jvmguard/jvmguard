package com.jvmguard.agent.data;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.parameter.RejectParameter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RejectResult extends BaseResult {
    private String hostName;
    private int port;

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        RejectParameter parameter = (RejectParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER);
        JvmGuardAgent.println(parameter.getErrorMessage());
        context.setTerminate(true);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        hostName = (String)context.getProperty(CommunicationContext.PROPERTY_REMOTE_HOST_NAME);
        port = (Integer)context.getProperty(CommunicationContext.PROPERTY_REMOTE_PORT);
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }
}
