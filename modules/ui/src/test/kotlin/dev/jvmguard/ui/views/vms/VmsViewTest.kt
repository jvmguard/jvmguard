package dev.jvmguard.ui.views.vms

import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.treegrid.TreeGrid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VmsViewTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setCurrent(UserSession(MockConnections.create()))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    @Test
    fun rendersVmTreeWithGroups() {
        UI.getCurrent().navigate(VmsView::class.java)
        val grid = find<TreeGrid<*>>().single()
        assertFalse(grid.treeData.rootItems.isEmpty())
    }

    @Test
    fun addsAColumnPerDefaultTelemetryType() {
        UI.getCurrent().navigate(VmsView::class.java)
        val grid = find<TreeGrid<*>>().single()
        assertTrue(grid.columns.size > 2)
    }
}
