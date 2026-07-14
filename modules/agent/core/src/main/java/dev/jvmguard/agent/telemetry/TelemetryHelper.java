package dev.jvmguard.agent.telemetry;

import dev.jvmguard.annotation.TelemetryFormat;
import dev.jvmguard.annotation.Unit;

import java.io.IOException;

public class TelemetryHelper {

    public static final TelemetryFormatImpl DEFAULT_FORMAT = new TelemetryFormatImpl(Unit.PLAIN, false, true, 0);

    private static final int UNIT_PLAIN = 1;
    private static final int UNIT_PER_SECOND = 2;
    private static final int UNIT_PERCENT = 3;
    private static final int UNIT_MICROSECONDS = 4;
    private static final int UNIT_NANOSECONDS = 5;
    private static final int UNIT_BYTES = 6;
    private static final int UNIT_MILLISECONDS = 7;

    public static String getIdentifier(String telemetryName, String lineName) {
        String name = telemetryName.replace('\t', ' ');
        if (lineName != null && !lineName.isEmpty()) {
            name += '\t' + lineName.replace('\t', ' ');
        }
        return name;
    }

    public static int getUnitIntValue(Unit unit) throws IOException {
        if (unit == null) {
            return UNIT_PLAIN;
        }
        switch (unit) {
            case PER_SECOND:
                return UNIT_PER_SECOND;
            case PERCENT:
                return UNIT_PERCENT;
            case MILLISECONDS:
                return UNIT_MILLISECONDS;
            case MICROSECONDS:
                return UNIT_MICROSECONDS;
            case NANOSECONDS:
                return UNIT_NANOSECONDS;
            case BYTES:
                return UNIT_BYTES;
            case PLAIN:
            default:
                return UNIT_PLAIN;
        }
    }

    public static Unit getUnitFromIntValue(int val) throws IOException {
        switch (val) {
            case UNIT_PER_SECOND:
                return Unit.PER_SECOND;
            case UNIT_PERCENT:
                return Unit.PERCENT;
            case UNIT_MILLISECONDS:
                return Unit.MILLISECONDS;
            case UNIT_MICROSECONDS:
                return Unit.MICROSECONDS;
            case UNIT_NANOSECONDS:
                return Unit.NANOSECONDS;
            case UNIT_BYTES:
                return Unit.BYTES;
            case UNIT_PLAIN:
            default:
                return Unit.PLAIN;
        }
    }

    public static boolean isFormatEqual(TelemetryFormat format1, TelemetryFormat format2) {
        return format1.stacked() == format2.stacked() && format1.scale() == format2.scale() && format1.value() == format2.value() && format1.groupAverage() == format2.groupAverage();
    }
}
