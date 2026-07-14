package dev.jvmguard.agent.mbean;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.agent.config.base.DefaultConstructor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MBeanDataParameter extends MBeanParameter {
    private boolean fetchValues;
    private boolean fetchStructure;

    @DefaultConstructor
    public MBeanDataParameter() {
    }

    public MBeanDataParameter(String name, boolean fetchStructure, boolean fetchValues) {
        super(name);
        this.fetchStructure = fetchStructure;
        this.fetchValues = fetchValues;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        super.write(context, out);
        out.writeBoolean(fetchStructure);
        out.writeBoolean(fetchValues);
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        super.read(context, in);
        fetchStructure = in.readBoolean();
        fetchValues = in.readBoolean();
    }

    public boolean isFetchValues() {
        return fetchValues;
    }

    public boolean isFetchStructure() {
        return fetchStructure;
    }
}
