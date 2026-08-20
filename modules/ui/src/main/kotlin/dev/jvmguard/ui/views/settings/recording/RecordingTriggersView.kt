package dev.jvmguard.ui.views.settings.recording

import dev.jvmguard.data.config.sets.TriggerSet
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.config.triggers.TriggerType
import dev.jvmguard.data.user.Roles
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.components.dropdownButton
import dev.jvmguard.ui.components.recording.RecordingGrid
import dev.jvmguard.ui.components.recording.triggerTypeIcon
import dev.jvmguard.ui.components.recording.triggers.TriggerGrid
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.enumLabel
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.Component
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.PROFILER)
@Route(value = "recording/triggers", layout = MainLayout::class)
class RecordingTriggersView : AbstractRecordingListView<Trigger, TriggerSet>() {

    override fun intro(selection: VmIdentifier): String? =
        if (selection.isRoot) null
        else t("recording.settings.triggers.intro")

    override val addButtonText: String get() = t("recording.settings.triggers.add")
    override val addButtonTestId: String get() = "trigger-add"
    override val setClass: Class<TriggerSet> get() = TriggerSet::class.java
    override val addSetSubtitle: String get() = t("recording.settings.set.addSubtitle.trigger")
    override val saveSetSubtitle: String get() = t("recording.settings.set.saveSubtitle.trigger")

    override fun items(selection: VmIdentifier): MutableList<Trigger>? =
        Sessions.recordingDraft().groupConfig(selection)?.triggerSettings?.triggers

    override fun onChanged() {
        val settings = Sessions.recordingDraft()
            .groupConfig(Sessions.recordingGroupSelection().selection)?.triggerSettings ?: return
        settings.triggers = settings.triggers
    }

    override fun createGrid(items: () -> MutableList<Trigger>, markChanged: () -> Unit): RecordingGrid =
        TriggerGrid(items, markChanged, ::thresholds, ::telemetryTypes)

    override fun createAddControl(grid: RecordingGrid): Component =
        dropdownButton(addButtonText, addButtonTestId) {
            TriggerType.entries.forEach { type ->
                item(triggerTypeIcon(type), enumLabel(type)) { (grid as TriggerGrid).addTrigger(type) }
            }
        }

    override fun loadSets(): Collection<TriggerSet> =
        Sessions.current()?.serverConnection?.triggerSets ?: emptyList()

    override fun newSet(name: String, items: List<Trigger>): TriggerSet = TriggerSet(name, items)

    private fun thresholds(): List<Threshold> =
        Sessions.recordingDraft().groupConfig(Sessions.recordingGroupSelection().selection)?.thresholdSettings?.thresholds
            ?: emptyList()

    private fun telemetryTypes(): Collection<TelemetryType> =
        Sessions.current()?.serverConnection?.idToTelemetryType?.values ?: emptyList()
}
