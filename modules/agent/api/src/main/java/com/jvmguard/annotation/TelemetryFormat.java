package com.jvmguard.annotation;

/**
 * Display options for a {@link Telemetry} annotation.
 * This annotation is used by the {@link Telemetry#format()} parameter.
 */
public @interface TelemetryFormat {
    /**
     * The unit of the recorded values. By default, telemetries are unitless.
     */
    Unit value() default Unit.PLAIN;

    /**
     * A scaling factor for the telemetry. In the UI recorded values will be multiplied with {@code 10^-scale}.
     * This can be useful to create fractional values from monitored integer values.
     * <p>
     * For example, if you want to show a percentage with two decimal digits, the monitored value should be
     * the percentage times 100 and the scale parameter should be set to {@code 2}.
     * </p>
     */
    int scale() default 0;

    /**
     * Determines whether multiple lines in a telemetry should be stacked into an area graph.
     * By default, multiple telemetry lines are not stacked. An example of a stacked graph is the "Heap usage" telemetry.
     */
    boolean stacked() default false;

    /**
     * By default, telemetry values from multiple VMs are averaged, for example, in the columns of the "VMs" view or
     * when you select a VM group for a telemetry in the "VM Data Views". If you want to show sums instead,
     * set this parameter to {@code true}.
     * <p>
     * The desired parameter value depends on the semantics of the monitored value. For shared resources,
     * you probably do not want to average values. For resources that exist on each machine, the sums from multiple
     * machines may not make much sense. For example, you would sum the monitored connections to a common database,
     * but average the monitored number of worker threads. Also, business numbers like the number of logged on users
     * will usually be summed.
     * </p>
     */
    boolean groupAverage() default true;
}
