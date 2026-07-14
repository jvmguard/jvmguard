package dev.jvmguard.ui.views.log

import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import dev.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class LogViewTest : JvmGuardBrowserlessTest() {

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    private fun login(level: AccessLevel) {
        Sessions.setCurrent(UserSession(MockConnections.create(level)))
    }

    // Per-log minimum level: server=admin, connection=profiler, event=viewer; below it the route guard forwards to VMs.
    @Test
    fun serverLogRequiresAdmin() {
        login(AccessLevel.ADMIN)
        UI.getCurrent().navigate(ServerLogView::class.java)
        assertInstanceOf(ServerLogView::class.java, currentView)

        login(AccessLevel.PROFILER)
        UI.getCurrent().navigate(ServerLogView::class.java)
        assertInstanceOf(VmsView::class.java, currentView)
    }

    @Test
    fun connectionLogRequiresProfiler() {
        login(AccessLevel.PROFILER)
        UI.getCurrent().navigate(ConnectionLogView::class.java)
        assertInstanceOf(ConnectionLogView::class.java, currentView)

        login(AccessLevel.VIEWER)
        UI.getCurrent().navigate(ConnectionLogView::class.java)
        assertInstanceOf(VmsView::class.java, currentView)
    }

    @Test
    fun eventLogAllowsViewer() {
        login(AccessLevel.VIEWER)
        UI.getCurrent().navigate(EventLogView::class.java)
        assertInstanceOf(EventLogView::class.java, currentView)
    }
}
