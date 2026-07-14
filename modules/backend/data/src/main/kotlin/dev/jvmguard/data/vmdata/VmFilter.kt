package dev.jvmguard.data.vmdata

enum class VmFilter(private val verbose: String) {
    CONNECTED("Connected JVMs"),
    RECENT("Recently seen JVMs");

    override fun toString(): String = verbose
}
