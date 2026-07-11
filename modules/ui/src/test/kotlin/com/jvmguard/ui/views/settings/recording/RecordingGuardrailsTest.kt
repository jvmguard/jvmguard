package com.jvmguard.ui.views.settings.recording

import com.jvmguard.data.config.guardrails.GuardrailSettings
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.textfield.IntegerField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordingGuardrailsTest : JvmGuardBrowserlessTest() {

    private lateinit var connection: ServerConnection

    @BeforeEach
    fun setUp() {
        connection = MockConnections.create(AccessLevel.ADMIN)
        Sessions.setCurrent(UserSession(connection))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.clearRecordingDraft()
        Sessions.resetRecordingSelection()
    }

    private fun rootGuardrails(): GuardrailSettings =
        Sessions.recordingDraft().groupConfig(VmIdentifier.ROOT_GROUP_IDENTIFIER)!!.guardrailSettings

    private fun checkbox(id: String): Checkbox = find<Checkbox>().all().first { it.testId == id }
    private fun integerField(id: String): IntegerField = find<IntegerField>().all().first { it.testId == id }

    @Test
    fun togglingACaptureAtRootStagesToTheRootGroup() {
        UI.getCurrent().navigate(RecordingGuardrailsView::class.java)

        assertTrue(rootGuardrails().allowHeapDump, "permissive by default")
        use(checkbox(RecordingGuardrailsView.ID_ALLOW_HEAP_DUMP)).click()

        assertFalse(rootGuardrails().allowHeapDump, "the toggle is staged on the root group")
        assertTrue(rootGuardrails().allowJps, "an unrelated toggle is untouched")
    }

    @Test
    fun togglingRunGcStagesToTheRootGroup() {
        UI.getCurrent().navigate(RecordingGuardrailsView::class.java)

        use(checkbox(RecordingGuardrailsView.ID_ALLOW_RUN_GC)).click()

        assertFalse(rootGuardrails().allowRunGc)
    }

    @Test
    fun togglingMbeanMutationsStagesToTheRootGroup() {
        UI.getCurrent().navigate(RecordingGuardrailsView::class.java)

        use(checkbox(RecordingGuardrailsView.ID_ALLOW_MBEAN_MUTATIONS)).click()

        assertFalse(rootGuardrails().allowMbeanMutations)
    }

    @Test
    fun togglingConfigEditStagesToTheRootGroup() {
        UI.getCurrent().navigate(RecordingGuardrailsView::class.java)

        use(checkbox(RecordingGuardrailsView.ID_ALLOW_CONFIG_EDIT)).click()

        assertFalse(rootGuardrails().allowConfigEdit)
    }

    @Test
    fun maxRecordingMinutesAreStoredAsSeconds() {
        UI.getCurrent().navigate(RecordingGuardrailsView::class.java)

        use(integerField(RecordingGuardrailsView.ID_MAX_RECORDING)).setValue(5)

        assertEquals(300, rootGuardrails().maxRecordingSeconds)
    }
}
