package dev.jvmguard.demo.server.services;

import dev.jvmguard.annotation.MethodTransaction;
import dev.jvmguard.annotation.Part;
import dev.jvmguard.annotation.Telemetry;
import dev.jvmguard.demo.server.Service;
import dev.jvmguard.demo.server.TrafficProfile;

import java.util.concurrent.atomic.AtomicInteger;

public class Cart extends Service {

    private static final AtomicInteger openCarts = new AtomicInteger();

    public Cart(TrafficProfile profile) {
        super(profile);
    }

    @Override
    public void start() {
        schedule(480, this::addItem, "cart-add");
        schedule(600, this::getCart, "cart-get");
        schedule(120, this::emptyCart, "cart-empty");
    }

    @MethodTransaction(naming = @Part(text = "Add cart item"))
    void addItem() {
        openLevel(openCarts, 20000, 80000);
        sleepMs(scale(8), scale(30));
    }

    @MethodTransaction(naming = @Part(text = "Get cart"))
    void getCart() {
        sleepMs(scale(5), scale(20));
    }

    @MethodTransaction(naming = @Part(text = "Empty cart"))
    void emptyCart() {
        openCarts.updateAndGet(v -> Math.max(0, v - 1));
        sleepMs(scale(5), scale(15));
    }

    @Telemetry("Open carts")
    public static int getOpenCarts() {
        return openCarts.get();
    }
}
