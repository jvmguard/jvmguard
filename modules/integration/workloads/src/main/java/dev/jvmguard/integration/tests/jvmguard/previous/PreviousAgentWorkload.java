package dev.jvmguard.integration.tests.jvmguard.previous;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.integration.AbstractJvmGuardRun;
import dev.jvmguard.integration.util.SleepHelper;

import java.lang.management.ManagementFactory;
import java.util.concurrent.locks.LockSupport;

public class PreviousAgentWorkload extends AbstractJvmGuardRun {
    @Override
    protected void work() throws InterruptedException {
        ManagementFactory.getPlatformMBeanServer();
        SleepHelper.sleep(1000 * 15);
        for (int i=0; i<10; i++) {
            transaction1(250);
            transaction2(250);
        }
        while (true) {
            LockSupport.park();
        }
    }

    @MethodTransaction
    public static void transaction1(int durationMillis) {
        SleepHelper.sleep(durationMillis);
    }

    @MethodTransaction
    public static void transaction2(int durationMillis) {
        SleepHelper.sleep(durationMillis);
    }

    @Telemetry(value = "test1")
    public static long getTelemetry() {
        return 10;
    }
}
