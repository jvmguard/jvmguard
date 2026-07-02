package com.jvmguard.agent.comm;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.jvmguard.agent.config.base.CheckedString;

import java.util.List;

public class JsonAgentWriter implements AgentWriter {
    private JsonObject current;

    public JsonAgentWriter(JsonObject root) {
        this.current = root;
    }

    @Override
    public void writeString(String name, String value) {
        current.put(name, value);
    }

    @Override
    public void writeInt(String name, int value) {
        current.put(name, value);
    }

    @Override
    public void writeLong(String name, long value) {
        current.put(name, value);
    }

    @Override
    public void writeBoolean(String name, boolean value) {
        current.put(name, value);
    }

    @Override
    public <E extends Enum<E>> void writeEnum(String name, E value) {
        current.put(name, value.name());
    }

    @Override
    public void writeCheckedString(String name, CheckedString value) {
        JsonObject nested = new JsonObject();
        nested.put("checked", value.isChecked());
        nested.put("value", value.getValue());
        current.put(name, nested);
    }

    @Override
    public void writeObject(String name, CodecEntity value) throws Exception {
        if (value != null) {
            current.put(name, writeBean(value));
        }
        // a null value omits the key; JsonAgentReader.readObject treats an absent key as null
    }

    @Override
    public <T extends CodecEntity> void writeList(String name, List<T> value) throws Exception {
        JsonArray array = new JsonArray();
        for (CodecEntity element : value) {
            array.add(writeBean(element));
        }
        current.put(name, array);
    }

    private JsonObject writeBean(CodecEntity bean) throws Exception {
        JsonObject nested = new JsonObject();
        nested.put("@type", bean.codecType());
        JsonObject previous = current;
        current = nested;
        bean.writeState(this);
        current = previous;
        return nested;
    }
}
