package dev.jvmguard.demo.server;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts events and snapshots an approximate per-second rate for use by {@code @Telemetry} getters.
 * <p>
 * The jvmguard telemetry collector polls the getter roughly once per second. {@link #perSecond()} returns
 * the number of events that occurred since the last call, scaled to a one-second window.
 */
public class RateMeter {

    private final AtomicLong count = new AtomicLong();
    private volatile long lastCount;
    private volatile long lastTime = System.currentTimeMillis();

    public void increment() {
        count.incrementAndGet();
    }

    public double perSecond() {
        long current = count.get();
        long now = System.currentTimeMillis();
        long previousTime = lastTime;
        long previousCount = lastCount;
        lastCount = current;
        lastTime = now;
        long dt = now - previousTime;
        return dt > 0 ? (current - previousCount) * 1000.0 / dt : 0;
    }
}
