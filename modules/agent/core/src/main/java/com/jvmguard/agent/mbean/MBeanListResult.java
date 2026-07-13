package com.jvmguard.agent.mbean;

import com.jvmguard.agent.comm.CommunicationContext;
import com.jvmguard.agent.data.BaseResult;
import com.jvmguard.mbean.data.MBeanManager;
import com.jvmguard.mbean.data.MBeanManager.NameResult;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MBeanListResult extends BaseResult {
    private static final boolean NO_PLATFORM_MBEAN = Boolean.getBoolean("jvmguard.noPlatformMBean");

    private Set<String> names = new HashSet<>();
    private boolean changed;

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        if (((MBeanListParameter)context.getProperty(CommunicationContext.PROPERTY_PARAMETER)).isCreatePlatformServer() && !NO_PLATFORM_MBEAN) {
            MBeanManager.createPlatformServer();
        }
        NameResult nameResult = MBeanManager.getMBeanNames();
        out.writeBoolean(nameResult.isChanged());
        if (nameResult.isChanged()) {
            writeNames(out, nameResult);
        }
    }

    private void writeNames(DataOutputStream out, NameResult nameResult) throws IOException {
        Set<String> names = nameResult.getNames();
        out.writeInt(names.size());
        for (String name : names) {
            out.writeUTF(name);
        }
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        changed = in.readBoolean();
        if (changed) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                names.add(in.readUTF());
            }
        }
    }

    public Set<String> getNames() {
        return names;
    }

    public boolean isChanged() {
        return changed;
    }
}
