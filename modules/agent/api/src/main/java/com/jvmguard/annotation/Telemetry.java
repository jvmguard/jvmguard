package com.jvmguard.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Create a Declared telemetry from the numeric return value of the annotated static method.
 * <p>
 * Only public static parameterless methods can be used for creating custom telemetries, annotating instance methods or non-public
 * static methods will not have any effect.
 * The method must return a numeric value, either a primitive value like {@code int} or {@code double} or a
 * primitive wrapper instance like {@code java.lang.Integer} or {@code java.lang.Double}.
 * </p>
 * <p>
 * It is not enough if the class containing the annotated method is present in the classpath.
 * <b>You have to make sure that the class is loaded at runtime</b>, so jvmguard can intercept the class loading
 * event and set up the custom telemetry.
 * </p>
 * <p>
 * The method is called periodically on a dedicated thread. This may have implications for the requirements of
 * <b>thread safety</b> in your code. If the numeric value is calculated on the fly, access to the data structures
 * must be made thread-safe by making access to those data structures synchronized. Alternatively, you could
 * calculate a cached value when convenient and save it to a volatile atomic value, like an {@code int} or a
 * class from the {@code java.util.concurrent.atomic} package.
 * </p>
 * <p>
 * In the jvmguard UI, the telemetry is shown in the VM data views under "Custom telemetries" together with the MBean
 * telemetries that are defined in the recording settings. In addition, you can
 * choose custom telemetries as data sources for sparklines in the dashboard or in the VMs view.
 * </p>
 * <p>
 * For example, the following code snippet defines a custom telemetry with the name "Connection count":
 * </p>
 * <pre>
 *     &#064;Telemetry("Connection count")
 *     public static int getConnectionCount() {
 *         return connectionCount;
 *     }</pre>
 * <p>
 * <b>Multiple lines in a single custom telemetry</b>
 * </p>
 * <p>
 * If you want to compare several measurements in the same telemetry, you can annotate multiple methods with the
 * same {@link #value()} for the telemetry name, but with a different {@link #line()} parameter.
 * </p>
 * <p>
 * For example:
 * </p>
 * <pre>
 *     &#064;Telemetry(value = "Queries", line = "Successful queries")
 *     public static int getSuccessCount() {
 *         return successCount;
 *     }
 *     &#064;Telemetry(value = "Queries", line = "Failed queries")
 *     public static int getErrorCount() {
 *         return errorCount;
 *     }</pre>
 * <p>
 * This will create a single custom telemetry with the name "Queries" and two data lines named
 * "Successful queries" and "Failed queries".
 * </p>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Telemetry {
    /**
     * The name of the telemetry. Give different names to different telemetries. Reuse the same name when annotating
     * different methods if you are creating a custom telemetry with multiple lines.
     * If only a single line is used, you can omit the "value" parameter name, as in
     * <pre>
     *     &#064;Telemetry("Connection count")</pre>
     */
    String value();

    /**
     * The optional line name of the telemetry. Only specify this parameter if you're creating a telemetry with
     * multiple lines. See {@link Telemetry} for an example.
     */
    String line() default "";

    /**
     * Optional display options for the telemetry.
     */
    TelemetryFormat format() default @TelemetryFormat;
}
