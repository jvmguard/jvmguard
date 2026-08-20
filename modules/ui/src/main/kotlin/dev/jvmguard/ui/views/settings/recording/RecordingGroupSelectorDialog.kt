package dev.jvmguard.ui.views.settings.recording

import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.t
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
    current, onSelect, { true }, t("recording.settings.selectGroup"),
    dialogWidth = "46rem", dialogHeight = "28rem", expandAll = true,
) {

    init {
        tree.addClassName("jvmguard-group-selector")
        add(Span(t("recording.settings.selector.legend")).apply {
            addClassName("jvmguard-field-hint")
            addClassName("jvmguard-selector-legend")
        })
    }

    override fun configureColumns() {
        addNameColumn().setHeader(t("recording.settings.selector.group")).setFlexGrow(1)
        OVERRIDE_CATEGORIES.forEach { category ->
            tree.addComponentColumn { overrideCell(it, category) }.apply {
                setHeader(t(category.headerKey))
                setAutoWidth(true)
                flexGrow = 0
                textAlign = ColumnTextAlign.CENTER
            }
        }
        tree.addComponentColumn(::triggersCell).apply {
            setHeader(t("nav.recording.triggers"))
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
        element.setAttribute("title", t("recording.settings.selector.defaultMarker"))
    }

    private class OverrideCategory(val headerKey: String, val isUsed: (GroupConfig) -> Boolean)

    companion object {
        private val OVERRIDE_CATEGORIES = listOf(
            OverrideCategory("nav.transactions") { it.transactionSettings.isUsed },
            OverrideCategory("nav.telemetries") { it.telemetrySettings.isUsed },
            OverrideCategory("nav.recording.thresholds") { it.thresholdSettings.isUsed },
            OverrideCategory("nav.recording.guardrails") { it.guardrailSettings.isUsed },
        )
    }
}
