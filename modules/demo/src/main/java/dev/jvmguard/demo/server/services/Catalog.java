package dev.jvmguard.demo.server.services;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.annotation.Part;
import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.annotation.TelemetryFormat;
import dev.jvmguard.annotation.TelemetryFormat.Unit;
import dev.jvmguard.demo.server.RateMeter;
import dev.jvmguard.demo.server.Service;
import dev.jvmguard.demo.server.TrafficProfile;

import java.util.concurrent.atomic.AtomicLong;

public class Catalog extends Service {

    private static final RateMeter queries = new RateMeter();
    private static final AtomicLong cacheHits = new AtomicLong();
    private static final AtomicLong cacheMisses = new AtomicLong();

    public Catalog(TrafficProfile profile) {
        super(profile);
    }

    @Override
    public void start() {
        schedule(600, this::listProducts, "catalog-list");
        schedule(900, this::getProduct, "catalog-get");
        schedule(180, this::searchProducts, "catalog-search");
    }

    @MethodTransaction(naming = @Part(text = "List products"))
    void listProducts() {
        queries.increment();
        sleepMs(scale(15), scale(60));
    }

    // fast on a cache hit, slower on a miss
    @MethodTransaction(naming = @Part(text = "Get product"))
    void getProduct() {
        queries.increment();
        boolean cacheHit = random.nextDouble() < 0.8;
        if (cacheHit) {
            cacheHits.incrementAndGet();
            sleepMs(scale(3), scale(15));
        } else {
            cacheMisses.incrementAndGet();
            sleepMs(scale(40), scale(120));
        }
    }

    @MethodTransaction(naming = @Part(text = "Search products"))
    void searchProducts() {
        queries.increment();
        sleepMs(scale(30), scale(150));
    }

    @Telemetry(value = "Catalog queries", format = @TelemetryFormat(Unit.PER_SECOND))
    public static double getQueriesPerSecond() {
        return queries.perSecond();
    }

    @Telemetry(value = "Cache hit rate", format = @TelemetryFormat(value = Unit.PERCENT, scale = 2))
    public static double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total == 0 ? 0 : hits * 10000.0 / total;
    }
}
