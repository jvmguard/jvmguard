package dev.jvmguard.annotation;

/**
 * Telemetry unit used in the {@link TelemetryFormat} annotation.
 */
public enum Unit {
    /**
     * The monitored value is a unitless number.
     */
    PLAIN,
    /**
     * The monitored value has the unit "per second". Conversion into the units "per-minute" and "per-hour" is
     * done automatically as required. In practice, this conversion would occur if {@link TelemetryFormat#scale()}
     * is set to a negative number.
     */
    PER_SECOND,
    /**
     * The monitored value has the unit "percent". To show two decimal digits, set {@link TelemetryFormat#scale()}
     * to {@code 2}.
     */
    PERCENT,
    /**
     * The monitored value has the unit "milliseconds". Conversion into higher unit prefixes such as seconds or minutes
     * is done automatically as required.
     */
    MILLISECONDS,
    /**
     * The monitored value has the unit "microseconds". Conversion into higher unit prefixes such as seconds or minutes
     * is done automatically as required.
     */
    MICROSECONDS,
    /**
     * The monitored value has the unit "nanoseconds". Conversion into higher unit prefixes such as microseconds or
     * milliseconds is done automatically as required.
     */
    NANOSECONDS,
    /**
     * The monitored value has the unit "bytes". Conversion into higher unit prefixes such as kB, MB or GB
     * is done automatically as required.
     */
    BYTES
}
