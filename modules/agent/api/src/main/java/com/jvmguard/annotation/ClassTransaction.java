package com.jvmguard.annotation;

import com.jvmguard.annotation.Inheritance.Mode;
import com.jvmguard.annotation.Part.Type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Create transactions for all public instance methods of the annotated class.
 * For any class that is instrumented with this annotation, all public methods will be instrumented except for methods
 * that are annotated with {@link NoTransaction @NoTransaction}.
 * Any invocation of an instrumented method will create a transaction with the specified {@link #naming() naming}.
 * <p>
 * Overridden methods in derived classes are not instrumented unless the {@link #inheritance()} parameter is set
 * accordingly. If you specify {@code @ClassTransaction} on an interface, you have to set {@link #inheritance()} to
 * something other than {@code Mode.NONE}, otherwise no method will be instrumented.
 * </p>
 * <p>
 * See the <a href="{@docRoot}/com/jvmguard/annotation/package-summary.html#package_description">package overview</a>
 * for an overview of the jvmguard API.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ClassTransaction {
    /**
     * Specify the name of the transactions that are created by the instrumented methods.
     * The name is expressed as a concatenation of name parts. See the documentation for
     * {@link Part} for a detailed explanation.
     * <p>
     * You can pass a single {@code @Part} or an array of parts with the syntax
     * {@code {@Part(...), @Part(...), @Part(...)}}.
     * </p>
     */
    Part[] naming() default @Part(Type.CLASS);

    /**
     * Specify a group name for the jvmguard UI configuration.
     * To configure options in the jvmguard UI that apply to different Declared transactions, set the group parameter
     * for those transactions to a common string. For example, policies are usually the same for a number
     * of transactions. By setting the group in the annotations, you can avoid multiple identical configuration
     * steps in UI.
     */
    String group() default "";

    /**
     * Specify how overridden methods in derived classes should be handled.
     * By default, overridden methods are not instrumented. This means that abstract methods in the annotated class
     * and overridden methods in derived classes that do not call {@code super()} will not generate any transactions.
     * <p>
     * If you set the inheritance parameter to something other than {@code Mode.NONE}, overridden and implemented
     * methods will be instrumented according to the rules in the {@code Inheritance} parameter.
     * See the documentation for {@link Inheritance} for a detailed explanation.
     * </p>
     */
    Inheritance inheritance() default @Inheritance(Mode.NONE);

    /**
     * Specify under what conditions this transaction should be created as a nested transaction.
     * If another transaction is underway, you might want to limit the creation of further
     * transactions. By default, no transactions with the same name are nested.
     */
    ReentryInhibition reentryInhibition() default ReentryInhibition.NAME;

    /**
     * Also intercept static methods. By default, only instance methods are instrumented. If this
     * property is set to {@code true}, public static methods create transactions as well.
     */
    boolean staticMethods() default false;
}
