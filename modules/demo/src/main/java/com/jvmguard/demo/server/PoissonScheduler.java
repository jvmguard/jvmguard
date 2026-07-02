package com.jvmguard.demo.server;

import java.util.Random;

/**
 * Drives a Poisson arrival process for a single operation type.
 * <p>
 * A dedicated platform thread sleeps for exponentially distributed intervals
 * (mean {@code 1/rate} minutes, where the rate comes from the {@link TrafficProfile}) and then launches
 * the operation body on a new virtual thread.
 */
public class PoissonScheduler {

    private static final long MINUTE_MS = 60_000L;

    public PoissonScheduler(double baseRatePerMinute, TrafficProfile profile, Runnable task, String threadName) {
        Thread.ofPlatform().name(threadName).daemon(true).start(() -> {
            Random random = new Random();
            while (true) {
                double rate = profile.rateFor(baseRatePerMinute);
                long intervalMs;
                if (rate <= 0.001) {
                    intervalMs = 60_000L;
                } else {
                    intervalMs = Math.round(-Math.log(1 - random.nextDouble()) / rate * MINUTE_MS);
                }
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    return;
                }
                try {
                    Thread.startVirtualThread(task);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }
}
