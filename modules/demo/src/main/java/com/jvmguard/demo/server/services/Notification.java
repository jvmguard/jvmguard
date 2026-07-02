package com.jvmguard.demo.server.services;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Telemetry;
import com.jvmguard.annotation.TelemetryFormat;
import com.jvmguard.annotation.Unit;
import com.jvmguard.demo.server.RateMeter;
import com.jvmguard.demo.server.Service;
import com.jvmguard.demo.server.TrafficProfile;

import java.util.concurrent.atomic.AtomicInteger;

public class Notification extends Service {

    private static final AtomicInteger queueDepth = new AtomicInteger();
    private static final RateMeter emailsSent = new RateMeter();

    public Notification(TrafficProfile profile) {
        super(profile);
    }

    @Override
    public void start() {
        schedule(300, this::sendOrderConfirmation, "notification-confirm");
        schedule(180, this::sendShippingUpdate, "notification-shipping");
    }

    @MethodTransaction(naming = @Part(text = "Send order confirmation"))
    void sendOrderConfirmation() {
        enqueueAndSend();
    }

    @MethodTransaction(naming = @Part(text = "Send shipping update"))
    void sendShippingUpdate() {
        enqueueAndSend();
    }

    private void enqueueAndSend() {
        queueDepth.incrementAndGet();
        try {
            sleepMs(scale(30), scale(120));
            emailsSent.increment();
        } finally {
            queueDepth.updateAndGet(v -> Math.max(0, v - 1));
        }
    }

    @Telemetry("Notification queue depth")
    public static int getQueueDepth() {
        return queueDepth.get();
    }

    @Telemetry(value = "Emails sent", format = @TelemetryFormat(Unit.PER_SECOND))
    public static double getEmailsPerSecond() {
        return emailsSent.perSecond();
    }
}
