package dev.jvmguard.demo.server;

@SuppressWarnings("unused")
public interface TrafficProfileMXBean {

    /**
     * Manual multiplier applied on top of the automatic, time-based modulation. Defaults to 1.
     */
    double getRateFactor();

    void setRateFactor(double rateFactor);

    /**
     * The current automatic rate modulation in [0, ~1], before rateFactor and flash-sale effects.
     */
    double getCurrentModulation();

    /**
     * Whether a site-wide flash sale is currently active.
     */
    boolean isFlashSale();
}
