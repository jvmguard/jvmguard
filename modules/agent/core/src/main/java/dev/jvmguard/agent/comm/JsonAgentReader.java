package dev.jvmguard.agent.comm;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import dev.jvmguard.agent.config.base.CheckedString;

import java.util.List;

public class JsonAgentReader implements AgentReader {
    private JsonObject current;

    public JsonAgentReader(JsonObject root) {
        this.current = root;
    }

    @Override
    public String readString(String name) {
        return current.getString(name, null);
    }

    @Override
    public int readInt(String name) {
        return current.getInt(name, 0);
    }

    @Override
    public long readLong(String name) {
        return current.getLong(name, 0);
    }

    @Override
    public boolean readBoolean(String name) {
        return current.getBoolean(name, false);
    }

    @Override
    public <E extends Enum<E>> E readEnum(String name, Class<E> enumClass) {
        return Enum.valueOf(enumClass, current.getString(name, null));
    }

    @Override
    public CheckedString readCheckedString(String name) {
        JsonObject nested = current.getObject(name);
        CheckedString checkedString = new CheckedString();
        if (nested != null) {
            checkedString.setChecked(nested.getBoolean("checked", false));
            checkedString.setValue(nested.getString("value", ""));
        }
        return checkedString;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CodecEntity> T readObject(String name) throws Exception {
        JsonObject nested = current.getObject(name);
        if (nested == null || current.isNull(name)) {
            return null;
        }
        return (T)readBean(nested);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CodecEntity> void readList(String name, List<T> list) throws Exception {
        JsonArray array = current.getArray(name);
        if (array != null) {
            for (Object element : array) {
                list.add((T)readBean((JsonObject)element));
            }
        }
    }

    private CodecEntity readBean(JsonObject nested) throws Exception {
        String type = nested.getString("@type", null);
        CodecEntity bean = CodecRegistry.create(type);
        JsonObject previous = current;
        current = nested;
        bean.readState(this);
        current = previous;
        return bean;
    }
}
