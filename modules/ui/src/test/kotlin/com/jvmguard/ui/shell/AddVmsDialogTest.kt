package com.jvmguard.ui.shell

import com.jvmguard.data.user.AccessLevel
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.select.Select
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AddVmsDialogTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.ADMIN)))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    private fun codeBlocks(): List<String> =
        find<Span>().all().filter { "jvmguard-code-text" in it.classNames }.map { it.text }

    @Test
    fun localDialogShowsTheAgentParameter() {
        AddLocalVmsDialog().open()
        assertTrue(codeBlocks().any { it.startsWith("-javaagent:") }, "the local agent parameter is shown")
    }

    @Test
    fun remoteDialogOffersDownloadAndServerParameter() {
        AddRemoteVmsDialog().open()
        assertFalse(find<Select<*>>().all().isEmpty(), "an archive-format select is shown")
        assertFalse(find<Anchor>().all().isEmpty(), "an agent download link is shown")
        assertTrue(codeBlocks().any { it.contains("server=") }, "the remote VM parameter references the server")
    }

    @Test
    fun viewerCannotAddVms() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.VIEWER)))
        openAddVms()
        assertTrue(find<AddVmsLocationDialog>().all().isEmpty())
        assertTrue(find<AddLocalVmsDialog>().all().isEmpty())
        assertTrue(find<AddRemoteVmsDialog>().all().isEmpty())
    }

    @Test
    fun headerButtonGatedByAccessLevel() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.VIEWER)))
        UI.getCurrent().navigate(VmsView::class.java)
        assertFalse(addVmsButton().isEnabled, "a viewer cannot add VMs")

        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.ADMIN)))
        UI.getCurrent().navigate(VmsView::class.java)
        assertTrue(addVmsButton().isEnabled, "an admin can add VMs")
    }

    private fun addVmsButton(): Button =
        find<Button>().all().first { it.text == "Add VMs" }
}
