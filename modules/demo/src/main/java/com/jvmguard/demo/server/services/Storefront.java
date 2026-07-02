package com.jvmguard.demo.server.services;

import com.jvmguard.annotation.MethodTransaction;
import com.jvmguard.annotation.Part;
import com.jvmguard.annotation.Telemetry;
import com.jvmguard.demo.server.Service;
import com.jvmguard.demo.server.TrafficProfile;

import java.util.concurrent.atomic.AtomicInteger;

// Customer facing edge service. Fans out to the catalog, recommendation, cart and checkout
public class Storefront extends Service {

    private static final AtomicInteger requestsInFlight = new AtomicInteger();
    private static final AtomicInteger activeSessions = new AtomicInteger();

    public Storefront(TrafficProfile profile) {
        super(profile);
    }

    @Override
    public void start() {
        schedule(600, this::browseHome, "storefront-browse");
        schedule(480, this::viewProduct, "storefront-view");
        schedule(180, this::searchProducts, "storefront-search");
        schedule(300, this::addToCart, "storefront-addcart");
        schedule(60, this::submitCheckout, "storefront-checkout");
    }

    private void track(Runnable op) {
        requestsInFlight.incrementAndGet();
        try {
            op.run();
        } finally {
            requestsInFlight.decrementAndGet();
        }
    }

    private void touchSession() {
        if (random.nextDouble() < 0.3) {
            openLevel(activeSessions, 30000, 120000);
        }
    }

    @MethodTransaction(naming = @Part(text = "Browse home"))
    void browseHome() {
        track(() -> {
            touchSession();
            renderPage();
            fetchRecommendations();
            fetchAds();
        });
    }

    @MethodTransaction(naming = @Part(text = "View product"))
    void viewProduct() {
        track(() -> {
            touchSession();
            loadProductDetails();
            fetchRecommendations();
        });
    }

    @MethodTransaction(naming = @Part(text = "Search products"))
    void searchProducts() {
        track(() -> {
            touchSession();
            executeSearch();
            fetchAds();
        });
    }

    @MethodTransaction(naming = @Part(text = "Add to cart"))
    void addToCart() {
        track(() -> {
            validateItem();
            updateCart();
        });
    }

    @MethodTransaction(naming = @Part(text = "Submit checkout"))
    void submitCheckout() {
        track(this::forwardToCheckout);
    }

    @MethodTransaction(naming = @Part(text = "Render page"))
    void renderPage() {
        sleepMs(scale(20), scale(80));
    }

    @MethodTransaction(naming = @Part(text = "Fetch recommendations"))
    void fetchRecommendations() {
        sleepMs(scale(10), scale(60));
    }

    @MethodTransaction(naming = @Part(text = "Fetch ads"))
    void fetchAds() {
        sleepMs(scale(5), scale(30));
    }

    @MethodTransaction(naming = @Part(text = "Load product details"))
    void loadProductDetails() {
        sleepMs(scale(10), scale(50));
    }

    @MethodTransaction(naming = @Part(text = "Execute search"))
    void executeSearch() {
        sleepMs(scale(30), scale(120));
    }

    @MethodTransaction(naming = @Part(text = "Validate cart item"))
    void validateItem() {
        sleepMs(scale(5), scale(20));
    }

    @MethodTransaction(naming = @Part(text = "Update cart"))
    void updateCart() {
        sleepMs(scale(10), scale(40));
    }

    @MethodTransaction(naming = @Part(text = "Forward to checkout"))
    void forwardToCheckout() {
        sleepMs(scale(20), scale(90));
    }

    @Telemetry("Requests in flight")
    public static int getRequestsInFlight() {
        return requestsInFlight.get();
    }

    @Telemetry("Active sessions")
    public static int getActiveSessions() {
        return activeSessions.get();
    }
}
