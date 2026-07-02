package com.jvmguard.integration.util;

import java.util.concurrent.locks.LockSupport;

public class SleepHelper {
    public static boolean preciseSleep = true;

    public static void sleep(long millis) {
        long nanos = millis * 1000 * 1000;
        long sleptNanos = 0;
        long startTime = System.nanoTime();
        do {
            LockSupport.parkNanos(nanos - sleptNanos);
            sleptNanos = System.nanoTime() - startTime;
        } while (preciseSleep && nanos - sleptNanos > 1000 * 3000);
    }

    @SuppressWarnings("RedundantThrows")
    public static void sleepWithInterrupt(long millis) throws InterruptedException {
        sleep(millis);
    }
}
