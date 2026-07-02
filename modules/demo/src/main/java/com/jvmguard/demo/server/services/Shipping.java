package com.jvmguard.demo.server.services;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Telemetry;
import com.jvmguard.demo.server.Service;
import com.jvmguard.demo.server.TrafficProfile;

import java.util.concurrent.atomic.AtomicInteger;

public class Shipping extends Service {

    private static final AtomicInteger shipmentsQueued = new AtomicInteger();

    public Shipping(TrafficProfile profile) {
        super(profile);
    }

    @Override
    public void start() {
        schedule(300, this::getQuote, "shipping-quote");
        schedule(120, this::shipOrder, "shipping-ship");
        schedule(180, this::track, "shipping-track");
    }

    @MethodTransaction(naming = @Part(text = "Get shipping quote"))
    void getQuote() {
        sleepMs(scale(15), scale(70));
    }

    @MethodTransaction(naming = @Part(text = "Create shipment"))
    void shipOrder() {
        openLevel(shipmentsQueued, 10000, 40000);
        sleepMs(scale(40), scale(160));
    }

    @MethodTransaction(naming = @Part(text = "Track shipment"))
    void track() {
        sleepMs(scale(10), scale(40));
    }

    @Telemetry("Shipments queued")
    public static int getShipmentsQueued() {
        return shipmentsQueued.get();
    }
}
