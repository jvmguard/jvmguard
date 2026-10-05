package dev.jvmguard.agent.comm;

import dev.jvmguard.agent.config.base.CheckedString;

import java.io.DataInputStream;
import java.util.List;

public class BinaryAgentReader implements AgentReader {
    private final DataInputStream in;
    private final CommunicationContext context;

    public BinaryAgentReader(DataInputStream in) {
        this(in, null);
    }

    public BinaryAgentReader(DataInputStream in, CommunicationContext context) {
        this.in = in;
        this.context = context;
    }

    @Override
    public boolean satisfies(ProtocolRequirement requirement) {
        return context == null || context.satisfies(requirement);
    }

    @Override
    public String readString(String name) throws Exception {
        return in.readUTF();
    }

    @Override
    public int readInt(String name) throws Exception {
        return in.readInt();
    }

    @Override
    public long readLong(String name) throws Exception {
        return in.readLong();
    }

    @Override
    public boolean readBoolean(String name) throws Exception {
        return in.readBoolean();
    }

    @Override
    public <E extends Enum<E>> E readEnum(String name, Class<E> enumClass) throws Exception {
        return Enum.valueOf(enumClass, in.readUTF());
    }

    @Override
    public CheckedString readCheckedString(String name) throws Exception {
        CheckedString checkedString = new CheckedString();
        checkedString.read(in);
        return checkedString;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CodecEntity> T readObject(String name) throws Exception {
        if (!in.readBoolean()) {
            return null;
        }
        String type = in.readUTF();
        CodecEntity bean = CodecRegistry.create(type);
        bean.readState(this);
        return (T)bean;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CodecEntity> void readList(String name, List<T> list) throws Exception {
        while (in.readBoolean()) {
            String type = in.readUTF();
            CodecEntity bean = CodecRegistry.create(type);
            bean.readState(this);
            list.add((T)bean);
        }
    }
}
