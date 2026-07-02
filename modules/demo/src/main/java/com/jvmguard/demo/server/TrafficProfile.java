package com.jvmguard.demo.server;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Multi-timescale traffic rate model: {@code rate = baseRate * modulation * rateFactor * flashSale},
 * where {@code modulation} multiplies weekly, diurnal, hourly and minute-scale sines plus a per-day
 * jitter, all derived from the wall clock. A site-wide flash sale every 3h (6 min) triples the rate and
 * stretches latencies by 1.8x.
 */
public class TrafficProfile implements TrafficProfileMXBean {

    private static final double MIDNIGHT_BASE = 0.15;
    private static final double WEEKEND_TROUGH = 0.35;
    private static final double DAILY_JITTER_STD_DEV = 0.12;
    private static final double DAILY_JITTER_FLOOR = 0.6;

    private static final long MINUTE_MS = 60_000L;
    private static final long FLASH_SALE_PERIOD_MIN = 3 * 60;
    private static final long FLASH_SALE_DURATION_MIN = 6;
    private static final double FLASH_SALE_RATE_MULTIPLIER = 3.0;
    private static final double FLASH_SALE_LATENCY_MULTIPLIER = 1.8;

    private final String name;
    private final double peakHour;
    private final Random random = new Random();

    private volatile double rateFactor = 1.0;
    private int lastDayOfYear = -1;
    private double dailyJitter = 1.0;

    public TrafficProfile(String name, double peakHour) {
        this.name = name;
        this.peakHour = peakHour;
    }

    @Override
    public double getRateFactor() {
        return rateFactor;
    }

    @Override
    public void setRateFactor(double rateFactor) {
        this.rateFactor = rateFactor;
    }

    @Override
    public double getCurrentModulation() {
        return modulation();
    }

    @Override
    public boolean isFlashSale() {
        return flashSaleActive();
    }

    /**
     * Effective event rate (events per minute) for the given base rate, including the time-based
     * modulation, the manual rate factor and any active flash sale.
     */
    public double rateFor(double baseRate) {
        return baseRate * modulation() * rateFactor * flashSaleRateMultiplier();
    }

    /**
     * Latency stretch factor from scheduled events (currently only the flash sale). Workload methods
     * multiply their base latency by this to inflate response times during peaks.
     */
    public double latencyMultiplier() {
        return flashSaleActive() ? FLASH_SALE_LATENCY_MULTIPLIER : 1.0;
    }

    private double modulation() {
        LocalDateTime now = LocalDateTime.now();
        rollDailyJitter(now.getDayOfYear());
        double hourDecimal = now.toLocalTime().toSecondOfDay() / 3600.0;
        double modulation = weekly(now.getDayOfWeek(), hourDecimal) * diurnal(hourDecimal)
                * hourlyWave() * shortWave() * dailyJitter;
        return Math.max(0, modulation);
    }

    private double diurnal(double hourDecimal) {
        double bump = Math.max(0, Math.cos(Math.PI * (hourDecimal - peakHour) / 12.0));
        return MIDNIGHT_BASE + (1 - MIDNIGHT_BASE) * bump;
    }

    private double weekly(DayOfWeek dayOfWeek, double hourDecimal) {
        double dayIndex = (dayOfWeek.getValue() % 7) + hourDecimal / 24.0; // Sunday = 0 ... Saturday = 6
        double cosine = Math.cos(2 * Math.PI * (dayIndex / 7.0) + Math.PI);
        return WEEKEND_TROUGH + (1 - WEEKEND_TROUGH) * (0.5 + 0.5 * cosine);
    }

    private double hourlyWave() {
        double tMin = System.currentTimeMillis() / (double) MINUTE_MS;
        return 1 + 0.18 * Math.sin(2 * Math.PI * tMin / 90.0)
                + 0.12 * Math.sin(2 * Math.PI * tMin / 47.0 + 1.3);
    }

    private double shortWave() {
        double tMin = System.currentTimeMillis() / (double) MINUTE_MS;
        return 1 + 0.14 * Math.sin(2 * Math.PI * tMin / 11.0 + 0.7)
                + 0.10 * Math.sin(2 * Math.PI * tMin / 7.0 + 2.1);
    }

    private void rollDailyJitter(int dayOfYear) {
        if (dayOfYear != lastDayOfYear) {
            dailyJitter = Math.max(DAILY_JITTER_FLOOR, 1 + random.nextGaussian() * DAILY_JITTER_STD_DEV);
            lastDayOfYear = dayOfYear;
        }
    }

    private boolean flashSaleActive() {
        long minuteOfEpoch = System.currentTimeMillis() / MINUTE_MS;
        return (minuteOfEpoch % FLASH_SALE_PERIOD_MIN) < FLASH_SALE_DURATION_MIN;
    }

    private double flashSaleRateMultiplier() {
        return flashSaleActive() ? FLASH_SALE_RATE_MULTIPLIER : 1.0;
    }

    @Override
    public String toString() {
        return name + " (peak @" + peakHour + "h)";
    }
}
