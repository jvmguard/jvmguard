package com.jvmguard.ui.views.data

import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.ui.views.data.telemetry.VmTelemetryView
import com.jvmguard.ui.views.data.transactions.TransactionsView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.QueryParameters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VmSelectionPersistenceTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setCurrent(UserSession(MockConnections.create()))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    @Test
    fun aDeepLinkSeedsTheSelectionModel() {
        UI.getCurrent().navigate(
            TransactionsView::class.java, QueryParameters.simple(mapOf("vm" to "Database/DB 01"))
        )
        val selection = Sessions.vmSelectionModel().selection
        assertFalse(selection.isRoot, "the ?vm= deep link should seed the model")
        assertEquals("Database/DB 01", selection.name)
    }

    @Test
    fun selectionSurvivesAViewSwitchViaTheModel() {
        UI.getCurrent().navigate(
            TransactionsView::class.java, QueryParameters.simple(mapOf("vm" to "Database/DB 01"))
        )
        UI.getCurrent().navigate(VmTelemetryView::class.java)
        assertEquals("DB 01", breadcrumbCurrent())
    }

    @Test
    fun aBareDataViewUrlShowsTheModelSelection_rootByDefault() {
        UI.getCurrent().navigate(VmTelemetryView::class.java)
        assertEquals("All JVMs", breadcrumbCurrent())
    }

    private fun breadcrumbCurrent(): String? =
        find<VmBreadcrumb>().single().children.toList()
            .filterIsInstance<Span>()
            .firstOrNull { it.classNames.contains("jvmguard-breadcrumb-current") }
            ?.text
}
