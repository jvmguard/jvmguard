package com.jvmguard.agent.comm;

import com.jvmguard.agent.config.base.CheckedString;

import java.util.List;

public interface AgentWriter {
    void writeString(String name, String value) throws Exception;

    void writeInt(String name, int value) throws Exception;

    void writeLong(String name, long value) throws Exception;

    void writeBoolean(String name, boolean value) throws Exception;

    <E extends Enum<E>> void writeEnum(String name, E value) throws Exception;

    void writeCheckedString(String name, CheckedString value) throws Exception;

    void writeObject(String name, CodecEntity value) throws Exception;

    <T extends CodecEntity> void writeList(String name, List<T> value) throws Exception;
}
