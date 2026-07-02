package com.jvmguard.integration

import java.lang.annotation.Inherited

/** Lowest JDK major version that is supported by a test. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Inherited
annotation class MinJdk(val value: Int)
