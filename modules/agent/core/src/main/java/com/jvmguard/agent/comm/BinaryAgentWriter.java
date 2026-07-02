package com.jvmguard.agent.comm;

import com.jvmguard.agent.config.base.CheckedString;

import java.io.DataOutputStream;
import java.util.List;

public class BinaryAgentWriter implements AgentWriter {
    private final DataOutputStream out;

    public BinaryAgentWriter(DataOutputStream out) {
        this.out = out;
    }

    @Override
    public void writeString(String name, String value) throws Exception {
        out.writeUTF(value);
    }

    @Override
    public void writeInt(String name, int value) throws Exception {
        out.writeInt(value);
    }

    @Override
    public void writeLong(String name, long value) throws Exception {
        out.writeLong(value);
    }

    @Override
    public void writeBoolean(String name, boolean value) throws Exception {
        out.writeBoolean(value);
    }

    @Override
    public <E extends Enum<E>> void writeEnum(String name, E value) throws Exception {
        out.writeUTF(value.name());
    }

    @Override
    public void writeCheckedString(String name, CheckedString value) throws Exception {
        value.write(out);
    }

    @Override
    public void writeObject(String name, CodecEntity value) throws Exception {
        if (value == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeUTF(value.codecType());
            value.writeState(this);
        }
    }

    @Override
    public <T extends CodecEntity> void writeList(String name, List<T> value) throws Exception {
        for (CodecEntity element : value) {
            out.writeBoolean(true);
            out.writeUTF(element.codecType());
            element.writeState(this);
        }
        out.writeBoolean(false);
    }
}
