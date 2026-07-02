package com.jvmguard.integration.tests.jvmguard.telemetry;

import com.jvmguard.annotation.Telemetry;
import com.jvmguard.annotation.TelemetryFormat;
import com.jvmguard.annotation.Unit;
import com.jvmguard.integration.AbstractJvmGuardRun;
import com.jvmguard.integration.tests.jvmguard.telemetry.classes.CustomTel1;
import com.jvmguard.integration.tests.jvmguard.telemetry.classes.CustomTel2;

public class CustomTelemetryFormatWorkload extends AbstractJvmGuardRun {
    @Override
    protected void work() throws InterruptedException {
        if (getVmNo() == 1) {
            new CustomTel1();
        } else {
            sleep(1000 * 40);
            new CustomTel2();
        }
    }

    @Telemetry(value = "micro", format = @TelemetryFormat(value = Unit.MICROSECONDS))
    public static long getMicro() {
        return 100;
    }

    @Telemetry(value = "nano", format = @TelemetryFormat(value = Unit.NANOSECONDS, stacked = true))
    public static long geNano() {
        return 1000 * 1000;
    }

    @Telemetry(value = "millis", format = @TelemetryFormat(value = Unit.MILLISECONDS))
    public static long geMillis() {
        return 30 * 1000;
    }

    @Telemetry(value = "perSecond", format = @TelemetryFormat(value = Unit.PER_SECOND, scale = 1))
    public static long getPerSecond() {
        return 100;
    }

    @Telemetry(value = "percent", format = @TelemetryFormat(value = Unit.PERCENT, scale = -2))
    public static double getPercent() {
        return 0.35d;
    }

    @Telemetry(value = "plain", format = @TelemetryFormat(value = Unit.PLAIN, groupAverage = false))
    public static double getPlain() {
        return 25;
    }


    @Telemetry(value = "stacked", line = "line 1")
    public static double getStacked1() {
        return 25;
    }

    @Telemetry(value = "stacked", line = "line 2", format = @TelemetryFormat(stacked = true))
    public static double getStacked2() {
        return 75;
    }

    @Telemetry(value = "nonAveraged", line = "line 1")
    public static double getNonGroupAveraged1() {
        return 25;
    }

    @Telemetry(value = "nonAveraged", line = "line 2", format = @TelemetryFormat(groupAverage = false))
    public static double getNonGroupAveraged2() {
        return 75;
    }
}
