package com.jvmguard.demo.server;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Service {

    protected final TrafficProfile profile;
    protected final Random random = new Random();

    protected Service(TrafficProfile profile) {
        this.profile = profile;
    }

    /**
     * Start all operation schedulers and any background tasks.
     */
    public abstract void start();

    /**
     * Schedule an operation as a Poisson arrival process with the given base rate (events per minute).
     */
    protected void schedule(double baseRatePerMinute, Runnable operation, String threadName) {
        new PoissonScheduler(baseRatePerMinute, profile, operation, threadName);
    }

    /**
     * Sleep for {@code millis} milliseconds, simulating I/O / DB / network latency without CPU load.
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sleep for a uniformly random duration in {@code [minMs, maxMs]}.
     */
    protected void sleepMs(int minMs, int maxMs) {
        sleep(minMs + random.nextInt(maxMs - minMs + 1));
    }

    /**
     * Current latency stretch factor (inflated during flash sales). Workload methods multiply their base
     * latency by this value.
     */
    protected double latencyScale() {
        return profile.latencyMultiplier();
    }

    /**
     * Scale a base latency (in ms) by the current latency stretch factor, floored at 1 ms.
     */
    protected int scale(int baseMs) {
        return Math.max(1, (int)Math.round(baseMs * latencyScale()));
    }

    /**
     * Increment a "level" gauge (open carts, active sessions, queued shipments ...) and schedule it to be
     * decremented after a random lifetime. This yields a steady-state level proportional to
     * {@code arrivalRate * avgLifetime}, so the gauge tracks traffic without growing unbounded or being
     * drained to zero by a separate decayer.
     */
    protected void openLevel(AtomicInteger gauge, int lifetimeMinMs, int lifetimeMaxMs) {
        gauge.incrementAndGet();
        Thread.startVirtualThread(() -> {
            sleep(lifetimeMinMs + random.nextInt(Math.max(1, lifetimeMaxMs - lifetimeMinMs + 1)));
            gauge.decrementAndGet();
        });
    }
}
