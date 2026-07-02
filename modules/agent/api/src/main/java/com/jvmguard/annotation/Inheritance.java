package com.jvmguard.annotation;

import java.lang.annotation.Target;

/**
 * Controls the way overridden methods will be handled by {@link MethodTransaction} and
 * {@link ClassTransaction}.
 * When a method is instrumented so that each invocation creates a transaction, there remains a choice with respect
 * to the handling of overridden methods in derived classes.
 * <p>
 * By default, overridden methods are ignored, so an overridden method that does not call {@code super()}, will not
 * create a transaction. This also applies to implementations of abstract methods, so if you annotate an interface
 * with {@code @ClassTransaction}, no transactions will be created by default.
 * </p>
 * <p>
 * By setting the {@link MethodTransaction#inheritance()} or {@link ClassTransaction#inheritance()} parameters to
 * something other than {@link Mode#NONE}, overridden and implemented methods will be considered as well.
 * If an overridden method calls {@code super()} in that case, you can prevent additional transactions by choosing
 * a naming scheme that does not differ for subclasses or by setting an appropriate {@link ReentryInhibition} value
 * for the transaction.
 * </p>
 * <p>
 * <b>Interfaces vs. proxies</b>
 * </p>
 * <p>
 * When inheritance is used and jvmguard processes an overridden or implementing method that is annotated as a
 * transaction in a super class or interface, there are two choices when adding class names to the transaction name:
 * <ul>
 * <li>
 * When you provide your own implementations of an <b>interface</b>, the implementations often have different tasks
 * that should be monitored separately. In that case, you want to see the derived class names in transaction names.
 * This is achieved by using {@link Mode#WITH_SUBCLASS_NAMES}.
 * <p>
 * For example:
 * </p>
 * <pre>
 *   &#064;MethodTransaction(inheritance = @Inheritance(Mode.WITH_SUBCLASS_NAMES))</pre>
 * <p>
 * Since {@code Mode.WITH_SUBCLASS_NAMES} is the default value of {@link Inheritance#value()},
 * you can leave it out and the previous example is equivalent to
 * </p>
 * <pre>
 *   &#064;MethodTransaction(inheritance = @Inheritance)</pre>
 * </li>
 * <li>
 * In a situation where a framework is creating <b>proxies</b> from an annotated class or interface, such as for an EJB, the
 * class name of the proxy class is not useful for monitoring. It often has an uninterpretable class name and changes
 * each time the VM is restarted or even more often. Similarly, you may be implementing your own interface, but the
 * implementations are all different implementations of the same task and should not be shown as separate transactions.
 * <p>
 * For these situations, you should use {@link Mode#WITH_SUPERCLASS_NAME}. In that mode, the name of the annotated
 * class will be used for transaction names. For example:
 * </p>
 * <pre>
 *   &#064;MethodTransaction(inheritance = @Inheritance(Mode.WITH_SUPERCLASS_NAME))</pre>
 * <p>
 * Since no explicit {@link MethodTransaction#naming() naming} has been specified, the default naming of
 * {@code @Part(Type.CLASS)} will be used, so there will only be a single transaction with the simple name of the
 * annotated class followed by the name of the annotated method.
 * </p>
 * </li>
 * </ul>
 * <p>
 * <b>Marker interfaces vs. regular interfaces</b>
 * <p>
 * For a {@link ClassTransaction}, all public methods are annotated. If
 * {@link Mode#WITH_SUBCLASS_NAMES} or
 * {@link Mode#WITH_SUPERCLASS_NAME} are used as the inheritance mode,
 * all public methods in derived classes are annotated as well, regardless of whether they override
 * or implement methods in the annotated class. This is desirable if the methods of interest
 * are not present in the super-class or interface. In the most extreme case, a marker interface has no methods at all,
 * and all methods are in derived classes.
 * </p>
 * <p>
 * If some public methods in derived classes should not be instrumented, you can use
 * {@link NoTransaction} to exclude entire classes or selected methods from
 * transaction processing.
 * </p>
 * <p>
 * However, in cases where the annotated class or interface already defines all methods of interest, you can
 * choose to ignore other public methods in derived classes by setting the {@link #implementingOnly()} parameter
 * to {@code true}. Only overriding or implementing methods in derived classes will then create transactions.
 * </p>
 * <p>
 * <b>Filters</b>
 * <p>
 * In the inheritance hierarchy, you may want to exclude certain classes. This is done with the {@link #filter()}
 * parameter. The filter is evaluated against the class name as determined by the {@link #value()} parameter
 * and can be a wildcard filter or a regular expression filter, depending on the value of the
 * {@link #filterType()} parameter.
 * </p>
 * <p>
 * For example, the
 * </p>
 * <pre>
 *   &#064;MethodTransaction(inheritance = @Inheritance(filter = "*Executor*"))</pre>
 * <p>
 * only instruments methods in classes that match the specified wildcard filter.
 * </p>
 */
@Target({})
public @interface Inheritance {
    /**
     * The inheritance mode. While the default value is {@link Mode#WITH_SUBCLASS_NAMES}, the default
     * inheritance mode of {@link MethodTransaction} and
     * {@link ClassTransaction} is {@link Mode#NONE}.
     * <p>
     * See {@link Inheritance} for a description of the different modes.
     * </p>
     */
    Mode value() default Mode.WITH_SUBCLASS_NAMES;

    /**
     * The filter for selecting derived classes.
     * By default, the filter is a wildcard filter with filter expression "*", so all derived classes are matched if
     * {@link #value()} is set to something other than {@link Mode#NONE}.
     * See {@link FilterType} for the syntax of filter expressions
     * and {@link Inheritance} for a description of filters.
     */
    String filter() default "*";

    /**
     * The type of the filter expression. By default,
     * {@link FilterType#WILDCARD wildcard filters} are used, but
     * {@link FilterType#REGEX regular expression filters} are also available for more complex
     * expressions.
     */
    FilterType filterType() default FilterType.WILDCARD;

    /**
     * Determines handling of methods in derived classes that are not present in the superclass.
     * This parameter only has an effect if used in the context of a {@link ClassTransaction} and
     * if {@link #value()} is set to {@link Mode#WITH_SUBCLASS_NAMES} or {@link Mode#WITH_SUPERCLASS_NAME}.
     * <p>
     * If set to {@code true}, only overriding or implementing public methods in derived classes are instrumented.
     * If set to {@code false}, all public methods in derived classes are instrumented. In that case, you can still
     * use {@link NoTransaction} to exclude selected derived classes or selected public methods in
     * them.
     * </p>
     */
    boolean implementingOnly() default false;

    /**
     * Mode for handling derived classes.
     * See {@link Inheritance} for a description of the different modes.
     */
    enum Mode {
        /**
         * No methods in derived classes will be instrumented.
         * Overridden methods that do not call {@code super()} do not create a transaction.
         */
        NONE,
        /**
         * All public methods in derived classes will be instrumented, using the <b>name of the derived class</b> for
         * transaction naming.
         * For a {@link ClassTransaction}, if {@link Inheritance#implementingOnly()} is set to
         * {@code true}, only overridden and implementing methods in derived classes create transactions.
         * @see Inheritance
         */
        WITH_SUBCLASS_NAMES,
        /**
         * All public methods in derived classes will be instrumented, using the <b>name of the annotated class</b> for
         * transaction naming.
         * For a {@link ClassTransaction}, if {@link Inheritance#implementingOnly()} is set to
         * {@code true}, only overridden and implementing methods in derived classes create transactions.
         * @see Inheritance
         */
        WITH_SUPERCLASS_NAME
    }
}
