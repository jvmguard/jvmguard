package dev.jvmguard.ui.server

import dev.jvmguard.data.vmdata.Telemetry
import dev.jvmguard.data.vmdata.TelemetryType

fun TelemetryType.displayName(): String {
    val known = KNOWN_NAMES[telemetryIdentifier.mainId to telemetryIdentifier.subId]
    return if (known != null && name == known.englishName) t(known.key) else name
}

private class KnownName(val key: String, val englishName: String)

private val KNOWN_NAMES: Map<Pair<String, String>, KnownName> = mapOf(
    (Telemetry.HEAP.mainId to TelemetryType.SUB_ID_USED_HEAP) to KnownName("telemetry.name.usedHeap", "Used Heap"),
    (Telemetry.HEAP.mainId to TelemetryType.SUB_ID_FREE_HEAP) to KnownName("telemetry.name.committedFreeHeap", "Committed Free Heap"),
    (Telemetry.HEAP.mainId to TelemetryType.SUB_ID_USED_HEAP_PERCENTAGE) to KnownName("telemetry.name.usedHeapPercentage", "Used Heap Percentage"),
    (Telemetry.CPU.mainId to "") to KnownName("telemetry.name.cpu", "CPU"),
    (Telemetry.GC.mainId to "") to KnownName("telemetry.name.gcActivity", "GC Activity"),
    (Telemetry.THREADS.mainId to "") to KnownName("telemetry.name.threadCount", "Thread Count"),
    (Telemetry.CONNECTIONS.mainId to "") to KnownName("telemetry.name.connectedVms", "Connected VMs"),
    (Telemetry.TRANSACTIONS.mainId to TelemetryType.SUB_ID_COMPLETED) to KnownName("telemetry.name.completedTransactions", "Completed Transactions"),
    (Telemetry.TRANSACTIONS.mainId to TelemetryType.SUB_ID_NORMAL) to KnownName("telemetry.name.normalTransactions", "Normal Transactions"),
    (Telemetry.TRANSACTIONS.mainId to TelemetryType.SUB_ID_SLOW) to KnownName("telemetry.name.slowTransactions", "Slow Transactions"),
    (Telemetry.TRANSACTIONS.mainId to TelemetryType.SUB_ID_VERY_SLOW) to KnownName("telemetry.name.verySlowTransactions", "Very Slow Transactions"),
    (Telemetry.TRANSACTIONS.mainId to TelemetryType.SUB_ID_ERROR) to KnownName("telemetry.name.errorTransactions", "Error Transactions"),
    (Telemetry.TRANSACTIONS.mainId to TelemetryType.SUB_ID_AVERAGE) to KnownName("telemetry.name.averageTransactionDuration", "Average Transaction Duration"),
)
