package com.jvmguard.ui.views.settings.recording

import com.jvmguard.agent.config.VmType
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.ui.views.settings.UsersView
import com.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RecordingSettingsTest : JvmGuardBrowserlessTest() {

    private val erp = VmIdentifier("ERP", VmType.GROUP)

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.clearRecordingDraft()
        Sessions.resetRecordingSelection()
    }

    private fun login(level: AccessLevel) {
        Sessions.setCurrent(UserSession(MockConnections.create(level)))
    }

    // find only matches effectively-visible components, so presence == visible here.
    private fun isOverride(checkbox: Checkbox): Boolean = checkbox.label?.startsWith("Override") == true
    private fun hasOverrideCheckbox(): Boolean = find<Checkbox>().all().any(::isOverride)
    private fun overrideCheckbox(): Checkbox = find<Checkbox>().all().first(::isOverride)
    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }

    // The cog ContextMenu is JS-connector-managed and not introspectable browserless, so assert the route guard.
    @Test
    fun generalSettingsRequireAdmin() {
        login(AccessLevel.ADMIN)
        UI.getCurrent().navigate(UsersView::class.java)
        assertInstanceOf(UsersView::class.java, currentView)

        login(AccessLevel.PROFILER)
        UI.getCurrent().navigate(UsersView::class.java)
        assertInstanceOf(VmsView::class.java, currentView)
    }

    @Test
    fun aViewerCannotOpenRecordingSettings() {
        login(AccessLevel.VIEWER)
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)
        assertInstanceOf(VmsView::class.java, currentView)
    }

    @Test
    fun aProfilerOpensRecordingSettings() {
        login(AccessLevel.PROFILER)
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)
        assertInstanceOf(RecordingTransactionsView::class.java, currentView)
    }

    @Test
    fun overrideCheckboxOnlyForNonRootGroups() {
        login(AccessLevel.ADMIN)
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)
        assertFalse(hasOverrideCheckbox(), "the root group defines defaults, no override")

        Sessions.recordingGroupSelection().set(erp)
        assertTrue(hasOverrideCheckbox(), "a child group can override")
    }

    @Test
    fun triggersViewHasNoOverrideCheckbox() {
        login(AccessLevel.ADMIN)
        Sessions.recordingGroupSelection().set(erp)
        UI.getCurrent().navigate(RecordingTriggersView::class.java)
        assertFalse(hasOverrideCheckbox())
    }

    @Test
    fun togglingOverridePersistsTheUsedFlag() {
        login(AccessLevel.ADMIN)
        val connection = Sessions.current()!!.serverConnection
        Sessions.recordingGroupSelection().set(erp)
        UI.getCurrent().navigate(RecordingTransactionsView::class.java)

        use(overrideCheckbox()).click()
        assertTrue(Sessions.recordingDraft().dirty, "toggling override marks the draft dirty")

        use(shellSave()).click()

        val saved = connection.groupConfigs.first { it.groupIdentifier == erp }
        assertTrue(saved.transactionSettings.isUsed, "the override was persisted to the group config")
    }
}
