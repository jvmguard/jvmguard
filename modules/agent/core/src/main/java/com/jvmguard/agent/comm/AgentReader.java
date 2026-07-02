package com.jvmguard.agent.comm;

import com.jvmguard.agent.config.base.CheckedString;

import java.util.List;

public interface AgentReader {
    String readString(String name) throws Exception;

    int readInt(String name) throws Exception;

    long readLong(String name) throws Exception;

    boolean readBoolean(String name) throws Exception;

    <E extends Enum<E>> E readEnum(String name, Class<E> enumClass) throws Exception;

    CheckedString readCheckedString(String name) throws Exception;

    <T extends CodecEntity> T readObject(String name) throws Exception;

    <T extends CodecEntity> void readList(String name, List<T> list) throws Exception;
}
