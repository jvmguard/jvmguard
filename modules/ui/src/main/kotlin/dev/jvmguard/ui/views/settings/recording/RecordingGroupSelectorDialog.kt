package dev.jvmguard.ui.views.settings.recording

import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.views.data.AbstractVmSelectorDialog
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.ColumnTextAlign
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.data.provider.hierarchy.TreeData

class RecordingGroupSelectorDialog(
    current: VmIdentifier,
    onSelect: (VmIdentifier) -> Unit,
) : AbstractVmSelectorDialog(
    current, onSelect, { true }, "Select group",
    dialogWidth = "46rem", dialogHeight = "28rem", expandAll = true,
) {

    init {
        tree.addClassName("jvmguard-group-selector")
        add(Span("✓ overrides the defaults     ●  defines the defaults").apply {
            addClassName("jvmguard-field-hint")
            addClassName("jvmguard-selector-legend")
        })
    }

    override fun configureColumns() {
        addNameColumn().setHeader("Group").setFlexGrow(1)
        OVERRIDE_CATEGORIES.forEach { category ->
            tree.addComponentColumn { overrideCell(it, category) }.apply {
                setHeader(category.header)
                setAutoWidth(true)
                flexGrow = 0
                textAlign = ColumnTextAlign.CENTER
            }
        }
        tree.addComponentColumn(::triggersCell).apply {
            setHeader("Triggers")
            setAutoWidth(true)
            flexGrow = 0
            textAlign = ColumnTextAlign.CENTER
        }
    }

    override fun buildTreeData(): TreeData<VmIdentifier> =
        treeDataOf(Sessions.recordingDraft().groupConfigs.map { it.groupIdentifier })

    private fun overrideCell(identifier: VmIdentifier, category: OverrideCategory): Component = when {
        identifier.isRoot -> defaultMarker()
        Sessions.recordingDraft().groupConfig(identifier)?.let(category.isUsed) == true -> checkIcon()
        else -> Span()
    }

    private fun triggersCell(identifier: VmIdentifier): Component {
        val count = Sessions.recordingDraft().groupConfig(identifier)?.triggerSettings?.activeTriggerCount ?: 0
        return Span(count.toString())
    }

    private fun checkIcon(): Component = VaadinIcon.CHECK.create().apply { setSize("1em") }

    private fun defaultMarker(): Component = Span("●").apply {
        addClassName("jvmguard-default-marker")
        element.setAttribute("title", "Defines the defaults")
    }

    private class OverrideCategory(val header: String, val isUsed: (GroupConfig) -> Boolean)

    companion object {
        private val OVERRIDE_CATEGORIES = listOf(
            OverrideCategory("Transactions") { it.transactionSettings.isUsed },
            OverrideCategory("Telemetries") { it.telemetrySettings.isUsed },
            OverrideCategory("Thresholds") { it.thresholdSettings.isUsed },
            OverrideCategory("Agent guardrails") { it.guardrailSettings.isUsed },
        )
    }
}
