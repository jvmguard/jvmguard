package com.jvmguard.ui.views.data.telemetry

import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.components.echart.EChart
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.ui.views.data.VmBreadcrumb
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.QueryParameters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VmTelemetryViewTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setCurrent(UserSession(MockConnections.create()))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    @Test
    fun breadcrumbDefaultsToAllJvmsWithoutVmParameter() {
        UI.getCurrent().navigate(VmTelemetryView::class.java)
        assertEquals(listOf("All JVMs"), breadcrumbLabels())
    }

    @Test
    fun breadcrumbReflectsTheVmQueryParameter() {
        UI.getCurrent().navigate(VmTelemetryView::class.java, QueryParameters.simple(mapOf("vm" to "ERP/Processing")))

        val labels = breadcrumbLabels()
        assertTrue(labels.contains("All JVMs"), labels.toString())
        assertTrue(labels.contains("ERP"), labels.toString())
        assertTrue(labels.contains("Processing"), labels.toString())
    }

    @Test
    fun groupSelectionRendersTheAggregateChart() {
        UI.getCurrent().navigate(VmTelemetryView::class.java)

        assertFalse(find<EChart>().all().isEmpty(), "a group selection must render the aggregate chart")
    }

    @Test
    fun singleVmSelectionRendersTheChart() {
        UI.getCurrent().navigate(VmTelemetryView::class.java, QueryParameters.simple(mapOf("vm" to "ERP/Processing")))

        assertFalse(find<EChart>().all().isEmpty(), "a single JVM selection must render the chart")
    }

    private fun breadcrumbLabels(): List<String> =
        find<VmBreadcrumb>().single().children.toList()
            .filterIsInstance<Span>()
            .map { it.text }
            .filter { it != "/" }
}
