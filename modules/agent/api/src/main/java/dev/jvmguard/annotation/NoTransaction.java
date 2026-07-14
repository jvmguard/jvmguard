package dev.jvmguard.annotation;

import dev.jvmguard.annotation.Inheritance.Mode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes the annotated method or class from the transaction detection.
 * If you use a {@link ClassTransaction} on a class, the invocations of all public methods
 * will be recorded as transactions. To exclude selected methods, annotate them with {@code @NoTransaction}.
 * <p>
 * If you set the {@link ClassTransaction#inheritance()} parameter to something other that
 * {@link Mode#NONE}, methods in derived classes will be instrumented as well.
 * You can annotate selected methods in derived classes with {@code @NoTransaction}, or you can annotate
 * entire derived classes with {@code @NoTransaction}. In the latter case, all methods of the entire derived class
 * will be excluded. However, this annotation is not inherited itself, so classes derived from a class that is
 * annotated with {@code @NoTransaction} are again eligible for transaction processing.
 * </p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface NoTransaction {
}
