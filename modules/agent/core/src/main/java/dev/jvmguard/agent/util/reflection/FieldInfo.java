package dev.jvmguard.agent.util.reflection;

import java.lang.reflect.Field;

public class FieldInfo {
    public final Field field;

    public FieldInfo(Field field) {
        this.field = field;
    }

    public Object getIfPossible(Object obj) {
        if (field != null) {
            try {
                return field.get(obj);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

}
