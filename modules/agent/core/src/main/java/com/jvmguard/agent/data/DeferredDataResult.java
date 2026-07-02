package com.jvmguard.agent.data;

import com.jvmguard.agent.comm.CommandType;
import com.jvmguard.agent.comm.CommunicationContext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

public class DeferredDataResult extends BaseResult {
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_INBOX_ALL = "inboxAll";

    public static final long NOT_AVAILABLE_ID = -1;

    private long id;
    private Properties properties;
    private CommandType commandType;
    private BaseResult data;

    @SuppressWarnings("UnusedDeclaration")
    public DeferredDataResult() {
    }

    public DeferredDataResult(long id, CommandType commandType, BaseResult data, Properties properties) {
        this.id = id;
        this.properties = properties;
        this.commandType = commandType;
        this.data = data;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    @Override
    public void write(CommunicationContext context, DataOutputStream out) throws IOException {
        DeferredDataResult deferredDataResult = (DeferredDataResult)context.getProperty(CommunicationContext.PROPERTY_DEFERRED_DATA);
        if (deferredDataResult == null) {
            realWrite(context, out);
        } else {
            deferredDataResult.realWrite(context, out);
        }
        context.setTerminate(true);
    }

    protected void realWrite(CommunicationContext context, DataOutputStream out) throws IOException {
        out.writeLong(id);
        if (properties == null) {
            out.writeInt(0);
        } else {
            out.writeInt(properties.size());
            for (Entry<Object, Object> entry : properties.entrySet()) {
                out.writeUTF(entry.getKey().toString());
                out.writeUTF(entry.getValue().toString());
            }
        }
        out.writeUTF(commandType.name());
        data.write(context, out);
    }

    @Override
    public void setTimestamp(long timestamp) {
        super.setTimestamp(timestamp);
        if (data != null) {
            data.setTimestamp(timestamp);
        }
    }

    @Override
    public void read(CommunicationContext context, DataInputStream in) throws IOException {
        id = in.readLong();
        properties = new Properties();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            String key = in.readUTF();
            String value = in.readUTF();
            properties.put(key, value);
        }
        commandType = CommandType.valueOf(in.readUTF());
        data = commandType.createResult();
        data.read(context, in);
    }

    @Override
    public void prepareDeferredLater(CommunicationContext context) {
        data.prepareDeferredLater(context);
    }

    @Override
    public void prepareDeferredDirect(CommunicationContext context) {
        data.prepareDeferredDirect(context);
    }

    public long getId() {
        return id;
    }

    public BaseResult getData() {
        return data;
    }

    public Properties getProperties() {
        return properties;
    }
}
