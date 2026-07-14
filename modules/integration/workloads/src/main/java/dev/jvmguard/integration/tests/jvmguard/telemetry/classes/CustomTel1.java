package dev.jvmguard.integration.tests.jvmguard.telemetry.classes;

import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.annotation.TelemetryFormat;
import dev.jvmguard.annotation.TelemetryFormat.Unit;

import java.util.Date;

public class CustomTel1 {
    static {
        System.out.println("TEL1 loaded " + new Date());
    }

    @Telemetry(value = "formatFromOne", line="line 1", format = @TelemetryFormat(value = Unit.BYTES, scale = 1, groupAverage = false, stacked = true))
    public static long getOne1() {
        return 100;
    }

    @Telemetry(value = "formatFromOne", line="line 2", format = @TelemetryFormat(value = Unit.MICROSECONDS))
    public static long getOne2() {
        return 200;
    }

    @Telemetry(value = "formatFromTwo", format = @TelemetryFormat(value = Unit.MICROSECONDS))
    public static long getTwo() {
        return 200;
    }
}
