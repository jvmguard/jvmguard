package dev.jvmguard.demo.server.services;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.annotation.Part;
import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.annotation.TelemetryFormat;
import dev.jvmguard.annotation.Unit;
import dev.jvmguard.demo.server.RateMeter;
import dev.jvmguard.demo.server.Service;
import dev.jvmguard.demo.server.TrafficProfile;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Checkout extends Service {

    private static final AtomicInteger checkoutsInProgress = new AtomicInteger();
    private static final RateMeter ordersPlaced = new RateMeter();

    public Checkout(TrafficProfile profile) {
        super(profile);
    }

    @Override
    public void start() {
        schedule(120, this::placeOrder, "checkout-place");
    }

    @MethodTransaction(naming = @Part(text = "Place order"))
    void placeOrder() {
        checkoutsInProgress.incrementAndGet();
        try {
            loadCart();
            prepareOrderItems();
            getShippingQuote();
            chargePayment();
            shipOrder();
            sendConfirmation();
            ordersPlaced.increment();
            if (nightlyBatchActive()) {
                // nightly order-processing batch as a long-tail latency outlier
                sleepMs(5000, 15000);
            }
        } finally {
            checkoutsInProgress.decrementAndGet();
        }
    }

    private boolean nightlyBatchActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.getHour() == 2 && now.getMinute() >= 10 && now.getMinute() < 25;
    }

    @MethodTransaction(naming = @Part(text = "Load cart"))
    void loadCart() {
        sleepMs(scale(10), scale(40));
    }

    @MethodTransaction(naming = @Part(text = "Prepare order items"))
    void prepareOrderItems() {
        sleepMs(scale(20), scale(80));
    }

    @MethodTransaction(naming = @Part(text = "Get shipping quote"))
    void getShippingQuote() {
        sleepMs(scale(15), scale(60));
    }

    @MethodTransaction(naming = @Part(text = "Charge payment"))
    void chargePayment() {
        sleepMs(scale(30), scale(120));
    }

    @MethodTransaction(naming = @Part(text = "Create shipment"))
    void shipOrder() {
        sleepMs(scale(20), scale(90));
    }

    @MethodTransaction(naming = @Part(text = "Send order confirmation"))
    void sendConfirmation() {
        sleepMs(scale(10), scale(50));
    }

    @Telemetry("Checkouts in progress")
    public static int getCheckoutsInProgress() {
        return checkoutsInProgress.get();
    }

    @Telemetry(value = "Orders placed", format = @TelemetryFormat(Unit.PER_SECOND))
    public static double getOrdersPerSecond() {
        return ordersPlaced.perSecond();
    }
}
