package dev.jvmguard.build

import org.gradle.jvm.toolchain.JavaToolchainService
import javax.inject.Inject

const val JAVA_BASELINE_VERSION = 25

fun releaseOf(version: String): Int = when (version) {
    "1.8" -> 8
    "11" -> 11
    "17" -> 11
    "21" -> 21
    "25" -> 25
    else -> throw RuntimeException("Java version $version not supported")
}

interface InjectedJavaToolchainService {
    @get:Inject
    val toolchains: JavaToolchainService
}
