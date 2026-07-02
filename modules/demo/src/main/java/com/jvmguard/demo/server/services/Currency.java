package com.jvmguard.demo.server.services;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Telemetry;
import com.jvmguard.annotation.TelemetryFormat;
import com.jvmguard.annotation.Unit;
import com.jvmguard.demo.server.RateMeter;
import com.jvmguard.demo.server.Service;
import com.jvmguard.demo.server.TrafficProfile;

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
