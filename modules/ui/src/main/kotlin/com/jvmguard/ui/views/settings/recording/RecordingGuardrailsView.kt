package com.jvmguard.ui.views.settings.recording

import com.jvmguard.agent.config.base.OptionalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.guardrails.GuardrailSettings
import com.jvmguard.data.user.Roles
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.settings.settingsSection
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.PROFILER)
@Route(value = "recording/guardrails", layout = MainLayout::class)
@PageTitle("jvmguard: Recording")
class RecordingGuardrailsView : AbstractRecordingSettingsView() {

    override val overrideCategory: (GroupConfig) -> OptionalConfig get() = { it.guardrailSettings }
    override val overrideLabel: String get() = "Override agent guardrails for this group"

    private val allowHeapDump = checkbox("Allow heap dumps", ID_ALLOW_HEAP_DUMP) { s, v -> s.allowHeapDump = v }
    private val allowJps = checkbox("Allow JProfiler snapshots", ID_ALLOW_JPS) { s, v -> s.allowJps = v }
    private val allowJfr = checkbox("Allow JFR recordings", ID_ALLOW_JFR) { s, v -> s.allowJfr = v }
    private val allowMbeanMutations =
        checkbox("Allow MBean attribute writes and operations", ID_ALLOW_MBEAN_MUTATIONS) { s, v -> s.allowMbeanMutations = v }
    private val allowConfigEdit =
        checkbox("Allow editing the recording configuration", ID_ALLOW_CONFIG_EDIT) { s, v -> s.allowConfigEdit = v }

    private val maxRecordingMinutes =
        integerField("Maximum recording duration (minutes, 0 = no limit)", ID_MAX_RECORDING, 1000) { s, v ->
            s.maxRecordingSeconds = v * 60
        }
    private val captureCooldown =
        integerField("Minimum seconds between captures on a VM (0 = no limit)", ID_CAPTURE_COOLDOWN, null) { s, v ->
            s.captureCooldownSeconds = v
        }

    private var contentBuilt = false

    override fun onSelectionChanged(selection: VmIdentifier) {
        if (!contentBuilt) {
            content.removeAll()
            content.add(
                settingsSection("Diagnostic captures", allowHeapDump, allowJps, allowJfr, maxRecordingMinutes, captureCooldown),
                settingsSection("Mutating actions", allowMbeanMutations, allowConfigEdit),
            )
            contentBuilt = true
        }
        val settings = guardrailSettings(selection) ?: return
        // Programmatic assignments have isFromClient == false, so they do not mark the draft as modified
        allowHeapDump.value = settings.allowHeapDump
        allowJps.value = settings.allowJps
        allowJfr.value = settings.allowJfr
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
        const val ID_ALLOW_MBEAN_MUTATIONS = "guardrails-allow-mbean-mutations"
        const val ID_ALLOW_CONFIG_EDIT = "guardrails-allow-config-edit"
        const val ID_MAX_RECORDING = "guardrails-max-recording"
        const val ID_CAPTURE_COOLDOWN = "guardrails-capture-cooldown"
    }
}
