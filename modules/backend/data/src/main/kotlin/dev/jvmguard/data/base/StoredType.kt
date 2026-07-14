package dev.jvmguard.data.base

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StoredType(val value: String)
