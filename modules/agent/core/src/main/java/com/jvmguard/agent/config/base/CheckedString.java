package com.jvmguard.agent.config.base;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class CheckedString implements Serializable {
    private boolean checked;
    private String value = "";

    public CheckedString(boolean checked, String value) {
        this.checked = checked;
        setValue(value);
    }

    public CheckedString() {
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
    }

    public String getUsedValue() {
        return getOrDefault("");
    }

    public String getOrDefault(String defaultValue) {
        return checked ? value : defaultValue;
    }

    public void setUsedValue(String value) {
        setValue(value);
        checked = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CheckedString that = (CheckedString)o;

        if (checked != that.checked) {
            return false;
        }
        if (!value.equals(that.value)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (checked ? 1 : 0);
        result = 31 * result + value.hashCode();
        return result;
    }

    public void read(DataInputStream in) throws IOException {
        checked = in.readBoolean();
        value = in.readUTF().intern();
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeBoolean(checked);
        out.writeUTF(value);
    }

    @Override
    public String toString() {
        return "CheckedString{" +
            "checked=" + checked +
            ", value='" + value + '\'' +
            '}';
    }
}
