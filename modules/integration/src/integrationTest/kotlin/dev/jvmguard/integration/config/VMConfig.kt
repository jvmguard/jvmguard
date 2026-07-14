package dev.jvmguard.integration.config

class VMConfig(private val majorVersion: Int) {

    val name: String get() = "${majorVersion}_64"

    fun isAtLeastJava(version: Int) = majorVersion >= version

    fun isJava(version: Int) = majorVersion == version
}
