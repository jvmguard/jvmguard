package com.jvmguard.agent.data;

import com.jvmguard.agent.JvmGuardAgent;
import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.comm.CommunicationContext.Type;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConnectionInfo extends BaseResult {

    private String vmName;
    private String vmPool;
    private String vmGroup;
    private long instanceId;
    private Type type;
    private long buildVersion;

    public ConnectionInfo() {
    }

    public String getVmName() {
        return vmName;
    }

    public String getVmPool() {
        return vmPool;
    }

    public String getVmGroup() {
        return vmGroup;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public Type getType() {
        return type;
    }

    public long getBuildVersion() {
        return buildVersion;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeUTF(JvmGuardAgent.getVmName());
        out.writeUTF(JvmGuardAgent.getVmPool());
        out.writeUTF(JvmGuardAgent.getVmGroup());
        out.writeLong(JvmGuardAgent.getInstanceId());
        out.writeLong(JvmGuardAgent.getBuildVersion());
        out.writeUTF(context.getType().name());
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        vmName = in.readUTF();
        vmPool = in.readUTF();
        vmGroup = in.readUTF();
        instanceId = in.readLong();
        buildVersion = in.readLong();
        type = CommunicationContext.Type.valueOf(in.readUTF());
        context.setType(type);
    }

    @Override
    public String toString() {
        return "ConnectionInfo{" +
            "vmName='" + vmName + '\'' +
            ", vmGroup='" + vmGroup + '\'' +
            ", vmPool='" + vmPool + '\'' +
            ", instanceId=" + instanceId +
            "}";
    }

}
