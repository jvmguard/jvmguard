package dev.jvmguard.agent.telemetry;

import dev.jvmguard.agent.base.logging.Subsystem;
import dev.jvmguard.agent.util.Logger;

import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

abstract class CustomTelemetry {
    private String name;

    public CustomTelemetry(String telemetryName, String lineName) {
        this.name = TelemetryHelper.getIdentifier(telemetryName, lineName);
    }

    protected abstract long getCurrentValue() throws Throwable;
    protected abstract int getType();
    protected abstract Subsystem getSubsystem();

    public String getName() {
        return name;
    }

    public void writeValue(DataOutput out) throws IOException {
        long currentValue;
        try {
            currentValue = getCurrentValue();
        } catch (IgnoreTelemetryException e) {
            return;
        } catch (Throwable e) {
            Logger.log(getSubsystem(), 0, true, e);
            return;
        }
        out.writeBoolean(true);
        out.writeInt(getType());
        out.writeUTF(name);
        out.writeLong(currentValue);
        writeFormat(out);
    }

    protected void writeFormat(DataOutput out) throws IOException {
        out.writeBoolean(false);
    }

    protected long convertNumber(Object value) throws IgnoreTelemetryException {
        if (value instanceof Number) {
            Number number = (Number)value;
            long ret;
            if (number instanceof Double || number instanceof Float) {
                ret = (long)(number.doubleValue() * 100);
            } else if (number instanceof BigDecimal) {
                ret = ((BigDecimal)number).movePointRight(2).longValue();
            } else {
                ret = number.longValue() * 100;
            }
            return ret;
        }
        Logger.log(getSubsystem(), 1, true, "no number value for %s: %s\n", this, (value == null ? "<null>" : value.getClass()));
        throw new IgnoreTelemetryException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomTelemetry)) {
            return false;
        }

        CustomTelemetry that = (CustomTelemetry)o;

        if (getType() != that.getType()) {
            return false;
        }
        if (!Objects.equals(name, that.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + getType();
        return result;
    }

    protected static class IgnoreTelemetryException extends Exception {
    }
}
