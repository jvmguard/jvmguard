package com.jvmguard.integration

import org.junit.jupiter.params.provider.ArgumentsSource

/** Passes the [JdkUnderTest]s from a [JdkArgumentsProvider] to a `@ParameterizedTest`. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@ArgumentsSource(JdkArgumentsProvider::class)
annotation class JdkSource
