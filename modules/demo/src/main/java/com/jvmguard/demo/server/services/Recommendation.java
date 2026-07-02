package com.jvmguard.demo.server.services;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Telemetry;
import com.jvmguard.annotation.TelemetryFormat;
import com.jvmguard.annotation.Unit;
import com.jvmguard.demo.server.RateMeter;
import com.jvmguard.demo.server.Service;
import com.jvmguard.demo.server.TrafficProfile;

import java.util.concurrent.atomic.AtomicLong;

public class Recommendation extends Service {

    private static final RateMeter recommendations = new RateMeter();
    private static final AtomicLong cacheHits = new AtomicLong();
    private static final AtomicLong cacheMisses = new AtomicLong();

    public Recommendation(TrafficProfile profile) {
        super(profile);
    }

    @Override
    public void start() {
        schedule(600, this::listRecommendations, "recommendation-list");
        schedule(0.02, this::warmCache, "recommendation-warm");
    }

    @MethodTransaction(naming = @Part(text = "List recommendations"))
    void listRecommendations() {
        recommendations.increment();
        boolean cacheHit = random.nextDouble() < 0.75;
        if (cacheHit) {
            cacheHits.incrementAndGet();
            sleepMs(scale(5), scale(25));
        } else {
            cacheMisses.incrementAndGet();
            sleepMs(scale(40), scale(160));
        }
    }

    @MethodTransaction(naming = @Part(text = "Warm recommendation cache"))
    void warmCache() {
        sleepMs(2000, 8000);
    }

    @Telemetry(value = "Recommendations served", format = @TelemetryFormat(Unit.PER_SECOND))
    public static double getRecommendationsPerSecond() {
        return recommendations.perSecond();
    }

    @Telemetry(value = "Cache hit rate", format = @TelemetryFormat(value = Unit.PERCENT, scale = 2))
    public static double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total == 0 ? 0 : hits * 10000.0 / total;
    }
}
