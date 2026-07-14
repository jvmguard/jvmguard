package dev.jvmguard.demo.server.services;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.annotation.Part;
import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.annotation.TelemetryFormat;
import dev.jvmguard.annotation.Unit;
import dev.jvmguard.demo.server.RateMeter;
import dev.jvmguard.demo.server.Service;
import dev.jvmguard.demo.server.TrafficProfile;

public class Payment extends Service {

    private static final RateMeter authorized = new RateMeter();
    private static final RateMeter declined = new RateMeter();

    private static final long MINUTE_MS = 60_000L;
    private static final long SURGE_PERIOD_MIN = 120;
    private static final long SURGE_DURATION_MIN = 5;

    public Payment(TrafficProfile profile) {
        super(profile);
    }

    @Override
    public void start() {
        schedule(120, this::charge, "payment-charge");
        schedule(6, this::refund, "payment-refund");
    }

    @MethodTransaction(naming = @Part(text = "Charge"))
    void charge() {
        authorize();
        boolean isDeclined = random.nextDouble() < (declineSurgeActive() ? 0.3 : 0.05);
        if (isDeclined) {
            declined.increment();
        } else {
            authorized.increment();
        }
        sleepMs(scale(80), scale(300));
    }

    @MethodTransaction(naming = @Part(text = "Authorize"))
    void authorize() {
        sleepMs(scale(20), scale(80));
    }

    @MethodTransaction(naming = @Part(text = "Refund"))
    void refund() {
        authorized.increment();
        sleepMs(scale(100), scale(400));
    }

    private boolean declineSurgeActive() {
        long minuteOfEpoch = System.currentTimeMillis() / MINUTE_MS;
        return (minuteOfEpoch % SURGE_PERIOD_MIN) < SURGE_DURATION_MIN;
    }

    @Telemetry(value = "Payments", line = "Authorized", format = @TelemetryFormat(value = Unit.PER_SECOND, stacked = true))
    public static double getAuthorizedPerSecond() {
        return authorized.perSecond();
    }

    @Telemetry(value = "Payments", line = "Declined", format = @TelemetryFormat(value = Unit.PER_SECOND, stacked = true))
    public static double getDeclinedPerSecond() {
        return declined.perSecond();
    }
}
