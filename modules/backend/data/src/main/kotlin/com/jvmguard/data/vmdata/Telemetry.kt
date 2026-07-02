package com.jvmguard.data.vmdata

enum class Telemetry(
    val mainId: String,
    private val verbose: String,
    val exportDescriptor: String,
) {
    CONNECTIONS("co", "Connected VMs", "connections"),
    TRANSACTIONS("tr", "Transactions", "transactions"),
    HEAP("hp", "Heap Usage", "heap"),
    CPU("cpu", "CPU Load", "cpu"),
    THREADS("th", "Thread Count", "threads"),
    GC("gc", "GC activity", "gc"),
    CUSTOM("cu", "Custom Telemetries", "custom");

    override fun toString(): String = verbose

    companion object {
        fun getByMainId(mainId: String): Telemetry? = entries.firstOrNull { it.mainId == mainId }

        fun isTransactionsTelemetry(mainId: String): Boolean = TRANSACTIONS.mainId == mainId
    }
}
