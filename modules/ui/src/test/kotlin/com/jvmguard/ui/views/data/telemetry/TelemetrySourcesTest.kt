package com.jvmguard.ui.views.data.telemetry

import com.jvmguard.ui.server.MockConnections
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TelemetrySourcesTest {

    @Test
    fun includesStandardTelemetriesWithConnectionsButNoProbes() {
        val sources = TelemetrySources.build(MockConnections.create())
        val mainIds = sources.filterIsInstance<MainIdTelemetrySource>().map { it.mainId }

        assertTrue("cpu" in mainIds, mainIds.toString())
        assertTrue("co" in mainIds, "Connected VMs missing: $mainIds")
        assertTrue("pdb" !in mainIds, "JDBC probe should be gone: $mainIds")
        assertTrue("pht" !in mainIds, "HTTP client probe should be gone: $mainIds")

        assertNotNull(TelemetrySources.byMainId(sources, "cpu"))
    }

    @Test
    fun includesCustomTelemetriesFromTheServer() {
        val labels = TelemetrySources.build(MockConnections.create())
            .filterIsInstance<CustomTelemetrySource>()
            .map { it.label }
        assertTrue("Request queue length" in labels, labels.toString())
        assertTrue("Cache entries" in labels, labels.toString())
    }

    @Test
    fun transactionsAreFlaggedForStatusColors() {
        val sources = TelemetrySources.build(MockConnections.create())
        val transactions = TelemetrySources.byMainId(sources, "tr")
        assertNotNull(transactions)
        assertTrue(transactions!!.isTransactions)
        assertTrue(TelemetrySources.byMainId(sources, "cpu")!!.isTransactions.not())
    }
}
