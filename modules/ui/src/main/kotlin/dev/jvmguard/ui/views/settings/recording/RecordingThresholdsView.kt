package dev.jvmguard.ui.views.settings.recording

import dev.jvmguard.agent.config.base.OptionalConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.sets.ThresholdSet
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.user.Roles
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.components.recording.RecordingGrid
import dev.jvmguard.ui.components.recording.thresholds.ThresholdGrid
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.PROFILER)
@Route(value = "recording/thresholds", layout = MainLayout::class)
@PageTitle("jvmguard: Recording")
class RecordingThresholdsView : AbstractRecordingListView<Threshold, ThresholdSet>() {

    override val overrideCategory: (GroupConfig) -> OptionalConfig get() = { it.thresholdSettings }
    override val overrideLabel: String get() = "Override threshold settings for this group"

    override val addButtonText: String get() = "Add threshold"
    override val addButtonTestId: String get() = "threshold-add"
    override val setClass: Class<ThresholdSet> get() = ThresholdSet::class.java
    override val singularSetName: String get() = "threshold set"
    override val pluralSetName: String get() = "threshold sets"
    override val addSetSubtitle: String get() = "The thresholds in the selected set are added to this group."
    override val saveSetSubtitle: String get() = "Saved threshold sets can be added to the thresholds of other groups."

    override fun items(selection: VmIdentifier): MutableList<Threshold>? =
        Sessions.recordingDraft().groupConfig(selection)?.thresholdSettings?.thresholds

    override fun createGrid(items: () -> MutableList<Threshold>, markChanged: () -> Unit): RecordingGrid =
        ThresholdGrid(items, markChanged, ::telemetryTypes)

    override fun loadSets(): Collection<ThresholdSet> =
        Sessions.current()?.serverConnection?.thresholdSets ?: emptyList()

    override fun newSet(name: String, items: List<Threshold>): ThresholdSet = ThresholdSet(name, items)

    private fun telemetryTypes(): Collection<TelemetryType> =
        Sessions.current()?.serverConnection?.idToTelemetryType?.values ?: emptyList()
}
