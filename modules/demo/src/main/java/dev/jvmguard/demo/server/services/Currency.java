package dev.jvmguard.demo.server.services;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.annotation.Part;
import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.annotation.TelemetryFormat;
import dev.jvmguard.annotation.TelemetryFormat.Unit;
import dev.jvmguard.demo.server.RateMeter;
import dev.jvmguard.demo.server.Service;
import dev.jvmguard.demo.server.TrafficProfile;

// High fan-out utility called by both the storefront and checkout. Had the highest base rate of all services.
// Each conversion is fast.
public class Currency extends Service {

    private static final RateMeter conversions = new RateMeter();

    public Currency(TrafficProfile profile) {
        super(profile);
    }

    @Override
    public void start() {
        schedule(1800, this::convert, "currency-convert");
        schedule(60, this::getSupportedCurrencies, "currency-list");
    }

    @MethodTransaction(naming = @Part(text = "Convert currency"))
    void convert() {
        conversions.increment();
        sleepMs(scale(2), scale(15));
    }

    @MethodTransaction(naming = @Part(text = "Get supported currencies"))
    void getSupportedCurrencies() {
        sleepMs(scale(5), scale(20));
    }

    @Telemetry(value = "Currency conversions", format = @TelemetryFormat(Unit.PER_SECOND))
    public static double getConversionsPerSecond() {
        return conversions.perSecond();
    }
}
