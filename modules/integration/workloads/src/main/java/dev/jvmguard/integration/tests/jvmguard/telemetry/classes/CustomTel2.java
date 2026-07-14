package dev.jvmguard.integration.tests.jvmguard.telemetry.classes;

import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.annotation.TelemetryFormat;
import dev.jvmguard.annotation.TelemetryFormat.Unit;

import java.util.Date;

public class CustomTel2 {
    static {
        System.out.println("TEL2 loaded " + new Date());
    }

    @Telemetry(value = "formatFromOne", line="line 1")
    public static long getOne1() {
        return 100;
    }

    @Telemetry(value = "formatFromOne", line="line 2")
    public static long getOne2() {
        return 200;
    }

    @Telemetry(value = "formatFromTwo", format = @TelemetryFormat(value = Unit.BYTES, stacked = true, scale = 1, groupAverage = false))
    public static long getTwo() {
        return 200;
    }
}
