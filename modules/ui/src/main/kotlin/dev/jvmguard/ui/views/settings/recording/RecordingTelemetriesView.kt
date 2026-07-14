package dev.jvmguard.ui.views.settings.recording

import dev.jvmguard.agent.config.base.OptionalConfig
import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.sets.TelemetrySet
import dev.jvmguard.data.user.Roles
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.components.recording.RecordingGrid
import dev.jvmguard.ui.components.recording.telemetries.TelemetryGrid
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.PROFILER)
@Route(value = "recording/telemetries", layout = MainLayout::class)
@PageTitle("jvmguard: Recording")
class RecordingTelemetriesView : AbstractRecordingListView<MBeanTelemetryConfig, TelemetrySet>() {

    override val overrideCategory: (GroupConfig) -> OptionalConfig get() = { it.telemetrySettings }
    override val overrideLabel: String get() = "Override telemetry settings for this group"

    override val addButtonText: String get() = "Add telemetry"
    override val addButtonTestId: String get() = "telemetry-add"
    override val setClass: Class<TelemetrySet> get() = TelemetrySet::class.java
    override val singularSetName: String get() = "telemetry set"
    override val pluralSetName: String get() = "telemetry sets"
    override val addSetSubtitle: String get() = "The telemetries in the selected set are added to this group."
    override val saveSetSubtitle: String get() = "Saved telemetry sets can be added to the telemetries of other groups."

    override fun items(selection: VmIdentifier): MutableList<MBeanTelemetryConfig>? =
        Sessions.recordingDraft().groupConfig(selection)?.telemetrySettings?.mbeanTelemetries

    override fun createGrid(items: () -> MutableList<MBeanTelemetryConfig>, markChanged: () -> Unit): RecordingGrid =
        TelemetryGrid(items, markChanged)

    override fun loadSets(): Collection<TelemetrySet> =
        Sessions.current()?.serverConnection?.telemetrySets ?: emptyList()

    override fun newSet(name: String, items: List<MBeanTelemetryConfig>): TelemetrySet = TelemetrySet(name, items)
}
