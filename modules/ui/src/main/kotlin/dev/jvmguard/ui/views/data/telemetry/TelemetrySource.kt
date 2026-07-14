package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.data.vmdata.*
import dev.jvmguard.connector.api.ServerConnection

sealed interface TelemetrySource {
    val key: String
    val label: String
    val isTransactions: Boolean
    val exportId: String

    fun fetch(connection: ServerConnection, vm: VM, interval: TelemetryInterval, endTime: Long): TelemetryData?
}

class MainIdTelemetrySource(
    val mainId: String,
    override val label: String,
    override val isTransactions: Boolean,
    override val exportId: String,
) : TelemetrySource {

    override val key: String get() = "m:$mainId"

    override fun fetch(connection: ServerConnection, vm: VM, interval: TelemetryInterval, endTime: Long): TelemetryData =
        connection.getTelemetryData(vm, mainId, interval, endTime)
}

class CustomTelemetrySource(
    val node: CustomTelemetryNodeIdentifier,
) : TelemetrySource {

    override val key: String get() = "c:${node.type}:${node.name}"
    override val label: String get() = node.name
    override val isTransactions: Boolean get() = false
    override val exportId: String get() = node.name

    override fun fetch(connection: ServerConnection, vm: VM, interval: TelemetryInterval, endTime: Long): TelemetryData =
        connection.getCustomTelemetryData(vm, node, interval, endTime)
}

object TelemetrySources {

    /** All chartable telemetry types for the connected server: standard (static) + custom (live). */
    fun build(connection: ServerConnection): List<TelemetrySource> {
        val sources = mutableListOf<TelemetrySource>()
        Telemetry.entries
            .filter { it != Telemetry.CUSTOM }
            .forEach {
                sources.add(
                    MainIdTelemetrySource(it.mainId, it.toString(), Telemetry.isTransactionsTelemetry(it.mainId), it.exportDescriptor)
                )
            }
        connection.customTelemetryInfo.customTelemetryNodeIdentifiers.forEach { node ->
            sources.add(CustomTelemetrySource(node))
        }
        return sources
    }

    fun byMainId(sources: List<TelemetrySource>, mainId: String): TelemetrySource? =
        sources.filterIsInstance<MainIdTelemetrySource>().firstOrNull { it.mainId == mainId }
}
