package dev.jvmguard.integration.tests.jvmguard.rest;

import dev.jvmguard.annotation.*;
import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.util.SleepHelper;

import java.lang.management.ManagementFactory;

public class RestWorkload extends AbstractJvmGuardRun {
    @Override
    protected void work() throws InterruptedException {
        ManagementFactory.getPlatformMBeanServer();
        while (true) {
            act();
        }
    }

    private void act() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            transaction1(500);
        }
    }

    @Telemetry(value = "vmNoSingle Щ")
    private static int getVmNoTelemetry() {
        return 100;
    }

    @Telemetry(value = "group1", line = "normal", format = @TelemetryFormat(value = Unit.BYTES, stacked = true, scale = 3, groupAverage = false))
    private static int getVmNoTelemetry2() {
        return 5000;
    }

    @Telemetry(value = "group1", line = "double")
    private static int getVmNoTelemetry3() {
        return 10000;
    }


    @MethodTransaction(naming = @Part(text = "transaction Щ"))
    public static void transaction1(int durationMillis) {
        SleepHelper.sleep(durationMillis);
    }

}
