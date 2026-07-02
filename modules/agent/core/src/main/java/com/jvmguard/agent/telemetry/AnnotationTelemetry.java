package com.jvmguard.agent.telemetry;

import com.jvmguard.agent.AgentConstants;
import com.jvmguard.agent.base.logging.Subsystem;
import com.jvmguard.annotation.Telemetry;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Method;

class AnnotationTelemetry extends CustomTelemetry {
    private Method method;
    private Telemetry telemetry;
    private boolean defaultFormat;

    AnnotationTelemetry(Method method, Telemetry telemetry) {
        super(telemetry.value(), telemetry.line());
        this.method = method;
        this.telemetry = telemetry;
        defaultFormat = TelemetryHelper.isFormatEqual(telemetry.format(), TelemetryHelper.DEFAULT_FORMAT);
    }

    @Override
    public String toString() {
        return "CustomTelemetry{" +
            "name=" + getName() +
            ", method=" + method +
            ", telemetry=" + telemetry +
            '}';
    }

    @Override
    protected long getCurrentValue() throws Throwable {
        return convertNumber(method.invoke(null));
    }

    @Override
    protected int getType() {
        return AgentConstants.TELEMETRY_TYPE_DEVOPS;
    }

    @Override
    protected Subsystem getSubsystem() {
        return Subsystem.COMMON;
    }

    @Override
    protected void writeFormat(DataOutput out) throws IOException {
        if (defaultFormat) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeByte(TelemetryHelper.getUnitIntValue(telemetry.format().value()));
            out.writeInt(telemetry.format().scale());
            out.writeBoolean(telemetry.format().stacked());
            out.writeBoolean(telemetry.format().groupAverage());
        }
    }
}
