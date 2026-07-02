package com.jvmguard.ui.views.data.telemetry

import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.components.echart.EChart
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.router.QueryParameters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TelemetryOverviewPanelTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setCurrent(UserSession(MockConnections.create()))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    @Test
    fun switchingModesSwapsTheChartAndOverviewGrid() {
        openOverview()

        assertFalse(find<EChart>().exists(), "the single-telemetry chart must be gone in overview mode")
        assertTrue(find<Grid<*>>().exists(), "the overview grid must be shown in overview mode")

        selectMode(0)
        assertTrue(find<EChart>().exists(), "the chart must return in telemetry mode")
        assertFalse(find<Grid<*>>().exists(), "the overview grid must be gone in telemetry mode")
    }

    @Test
    fun rendersEveryVisibleTelemetryForTheRoot() {
        openOverview()

        val names = rowNames()
        assertTrue("Heap size" in names, names.toString())
        assertTrue("CPU load" in names, names.toString())
        assertTrue(names.any { it.startsWith("Fake telemetry") }, names.toString())
        assertEquals(listOf("Heap size", "CPU load"), names.take(2), names.toString())
    }

    @Test
    fun rendersForASingleVm() {
        openOverview(mapOf("vm" to "Database/DB 01"))

        assertTrue(rowNames().isNotEmpty(), "single-JVM overview should render rows")
    }

    @Test
    fun rendersAggregateForAGroup() {
        openOverview(mapOf("vm" to "ERP/Processing"))

        assertTrue(rowNames().isNotEmpty(), "group overview should render rows")
    }

    private fun openOverview(params: Map<String, String> = emptyMap()) {
        UI.getCurrent().navigate(VmTelemetryView::class.java, QueryParameters.simple(params))
        selectMode(1)
    }

    private fun selectMode(index: Int) = use(find<Tabs>().single()).select(index)

    @Suppress("UNCHECKED_CAST")
    private fun rowNames(): List<String> {
        val grid = find<Grid<*>>().single() as Grid<TelemetryOverviewRow>
        val items = (grid.dataProvider as ListDataProvider<TelemetryOverviewRow>).items
        return items.map { it.name }
    }
}
