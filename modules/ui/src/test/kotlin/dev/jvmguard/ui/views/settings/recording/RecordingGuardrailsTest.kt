package dev.jvmguard.ui.views.settings.recording

import dev.jvmguard.data.config.guardrails.GuardrailSettings
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import dev.jvmguard.connector.api.ServerConnection
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
