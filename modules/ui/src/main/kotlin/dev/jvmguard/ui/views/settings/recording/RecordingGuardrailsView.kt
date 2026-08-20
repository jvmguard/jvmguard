package dev.jvmguard.ui.views.settings.recording

import dev.jvmguard.agent.config.base.OptionalConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.guardrails.GuardrailSettings
import dev.jvmguard.data.user.Roles
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.ui.views.settings.settingsSection
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.PROFILER)
@Route(value = "recording/guardrails", layout = MainLayout::class)
class RecordingGuardrailsView : AbstractRecordingSettingsView() {

    override val overrideCategory: (GroupConfig) -> OptionalConfig get() = { it.guardrailSettings }
    override val overrideLabel: String get() = t("recording.settings.guardrails.override")

    private val allowHeapDump = checkbox(t("recording.settings.guardrails.allowHeapDump"), ID_ALLOW_HEAP_DUMP) { s, v -> s.allowHeapDump = v }
    private val allowJps = checkbox(t("recording.settings.guardrails.allowJps"), ID_ALLOW_JPS) { s, v -> s.allowJps = v }
    private val allowJfr = checkbox(t("recording.settings.guardrails.allowJfr"), ID_ALLOW_JFR) { s, v -> s.allowJfr = v }
    private val redactSnapshots =
        checkbox(t("recording.settings.guardrails.redactSnapshots"), ID_REDACT_SNAPSHOTS) { s, v -> s.redactSnapshots = v }
    private val allowMbeanMutations =
        checkbox(t("recording.settings.guardrails.allowMbeanMutations"), ID_ALLOW_MBEAN_MUTATIONS) { s, v -> s.allowMbeanMutations = v }
    private val allowConfigEdit =
        checkbox(t("recording.settings.guardrails.allowConfigEdit"), ID_ALLOW_CONFIG_EDIT) { s, v -> s.allowConfigEdit = v }

    private val maxRecordingMinutes =
        integerField(t("recording.settings.guardrails.maxRecording"), ID_MAX_RECORDING, 1000) { s, v ->
            s.maxRecordingSeconds = v * 60
        }
    private val captureCooldown =
        integerField(t("recording.settings.guardrails.captureCooldown"), ID_CAPTURE_COOLDOWN, null) { s, v ->
            s.captureCooldownSeconds = v
        }

    private var contentBuilt = false

    override fun onSelectionChanged(selection: VmIdentifier) {
        if (!contentBuilt) {
            content.removeAll()
            content.add(
                settingsSection(
                    t("recording.settings.guardrails.sectionCaptures"),
                    allowHeapDump, allowJps, allowJfr, redactSnapshots, maxRecordingMinutes, captureCooldown
                ),
                settingsSection(t("recording.settings.guardrails.sectionMutating"), allowMbeanMutations, allowConfigEdit),
            )
            contentBuilt = true
        }
        val settings = guardrailSettings(selection) ?: return
        // Programmatic assignments have isFromClient == false, so they do not mark the draft as modified
        allowHeapDump.value = settings.allowHeapDump
        allowJps.value = settings.allowJps
        allowJfr.value = settings.allowJfr
        redactSnapshots.value = settings.redactSnapshots
        allowMbeanMutations.value = settings.allowMbeanMutations
        allowConfigEdit.value = settings.allowConfigEdit
        maxRecordingMinutes.value = settings.maxRecordingSeconds / 60
        captureCooldown.value = settings.captureCooldownSeconds
    }

    override fun onEditableChanged(editable: Boolean) {
        content.isEnabled = editable
    }

    private fun checkbox(label: String, testId: String, apply: (GuardrailSettings, Boolean) -> Unit): Checkbox =
        Checkbox(label).apply {
            this.testId = testId
            addValueChangeListener { event ->
                if (event.isFromClient) currentSettings()?.let { apply(it, value); markChanged() }
            }
        }

    private fun integerField(label: String, testId: String, max: Int?, apply: (GuardrailSettings, Int) -> Unit): IntegerField =
        IntegerField(label).apply {
            this.testId = testId
            addClassName("jvmguard-nowrap-label")
            min = 0
            max?.let { setMax(it) }
            width = "16rem"
            addValueChangeListener { event ->
                if (event.isFromClient) currentSettings()?.let { apply(it, value ?: 0); markChanged() }
            }
        }

    private fun currentSettings(): GuardrailSettings? =
        guardrailSettings(Sessions.recordingGroupSelection().selection)

    private fun guardrailSettings(selection: VmIdentifier): GuardrailSettings? =
        Sessions.recordingDraft().groupConfig(selection)?.guardrailSettings

    private fun markChanged() {
        Sessions.recordingDraft().markChanged(Sessions.recordingGroupSelection().selection)
    }

    companion object {
        const val ID_ALLOW_HEAP_DUMP = "guardrails-allow-heap-dump"
        const val ID_ALLOW_JPS = "guardrails-allow-jps"
        const val ID_ALLOW_JFR = "guardrails-allow-jfr"
        const val ID_REDACT_SNAPSHOTS = "guardrails-redact-snapshots"
        const val ID_ALLOW_MBEAN_MUTATIONS = "guardrails-allow-mbean-mutations"
        const val ID_ALLOW_CONFIG_EDIT = "guardrails-allow-config-edit"
        const val ID_MAX_RECORDING = "guardrails-max-recording"
        const val ID_CAPTURE_COOLDOWN = "guardrails-capture-cooldown"
    }
}
