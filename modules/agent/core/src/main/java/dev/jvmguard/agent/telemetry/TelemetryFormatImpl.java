package dev.jvmguard.agent.telemetry;

import dev.jvmguard.agent.comm.CommunicationContext;
import dev.jvmguard.annotation.TelemetryFormat;
import dev.jvmguard.annotation.Unit;

import java.io.DataInput;
import java.io.IOException;
import java.lang.annotation.Annotation;

@SuppressWarnings("ClassExplicitlyAnnotation")
public class TelemetryFormatImpl implements TelemetryFormat {
    private Unit unit;
    private boolean stacked;
    private boolean groupAverage;
    private int scale;

    public TelemetryFormatImpl(DataInput in, @SuppressWarnings("unused") CommunicationContext context) throws IOException {
        unit = TelemetryHelper.getUnitFromIntValue(in.readByte());
        scale = in.readInt();
        stacked = in.readBoolean();
        groupAverage = in.readBoolean();
    }

    public TelemetryFormatImpl(Unit unit, boolean stacked, boolean groupAverage, int scale) {
        this.unit = unit;
        this.stacked = stacked;
        this.groupAverage = groupAverage;
        this.scale = scale;
    }

    @Override
    public Unit value() {
        return unit;
    }

    @Override
    public int scale() {
        return scale;
    }

    @Override
    public boolean stacked() {
        return stacked;
    }

    @Override
    public boolean groupAverage() {
        return groupAverage;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return TelemetryFormat.class;
    }

    @Override
    public String toString() {
        return "FormatImpl{" +
            "unit=" + unit +
            ", scale=" + scale +
            ", stacked=" + stacked +
            '}';
    }
}
