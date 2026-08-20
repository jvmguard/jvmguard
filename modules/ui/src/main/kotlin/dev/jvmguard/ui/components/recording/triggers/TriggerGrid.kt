package dev.jvmguard.ui.components.recording.triggers

import dev.jvmguard.common.helper.DeepCopy
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.config.triggers.TriggerType
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.ui.components.*
import dev.jvmguard.ui.components.recording.RecordingGrid
import dev.jvmguard.ui.components.recording.triggerTypeIcon
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon

class TriggerGrid(
    private val triggers: () -> MutableList<Trigger>,
    private val markChanged: () -> Unit,
    private val thresholds: () -> List<Threshold>,
    private val telemetryTypes: () -> Collection<TelemetryType>,
) : RecordingGrid() {

    private val grid = Grid<Trigger>().apply {
        testId = ID_GRID
        addComponentColumn(::nameCell).setHeader(t("trigger.grid.header")).setFlexGrow(1)
        addComponentColumn(::rowActions).setKey(ACTIONS_KEY).setAutoWidth(true).setFlexGrow(0)
        setEmptyStateComponent(emptyState(t("trigger.grid.empty")))
        addItemDoubleClickListener { edit(it.item) }
        editDeleteKeys(::edit, ::delete)
        enableRowReorder(items = triggers, onReordered = ::changed)
        isAllRowsVisible = true
    }

    init {
        isPadding = false
        isSpacing = false
        setWidthFull()
        add(grid)
        refresh()
    }

    override fun addNew() = addTrigger(TriggerType.THRESHOLD)

    fun addTrigger(type: TriggerType) {
        // A threshold-violation trigger references a group threshold; without one there is nothing to pick.
        if (type == TriggerType.THRESHOLD && thresholds().isEmpty()) {
            Notifications.show(t("trigger.grid.defineThresholdFirst"))
            return
        }
        openDialog(type.createTrigger(), isNew = true) {
            triggers().add(it)
            changed()
        }
    }

    override fun refresh() {
        grid.setItems(triggers())
    }

    private fun edit(trigger: Trigger) {
        openDialog(DeepCopy.clone(trigger), isNew = false) { saved ->
            val index = triggers().indexOf(trigger)
            if (index >= 0) {
                triggers()[index] = saved
            }
            changed()
        }
    }

    private fun openDialog(trigger: Trigger, isNew: Boolean, onSave: (Trigger) -> Unit) {
        TriggerDialog(trigger, isNew, thresholds(), telemetryTypes(), onSave).open()
    }

    private fun nameCell(trigger: Trigger): Component {
        val icon = triggerTypeIcon(trigger.triggerType).create().apply { setSize("1.2em") }
        val name = Span(describe(trigger, telemetryTypes))
        return if (trigger.isEnabled) {
            cellRow(icon, name)
        } else {
            cellRow(icon, name, Span(t("trigger.disabled")).apply { addClassName("jvmguard-row-detail") })
        }
    }

    private fun rowActions(trigger: Trigger): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, t("recording.actions"), "trigger-row-menu-${triggers().indexOf(trigger)}") {
            addItem(t("common.edit")) { edit(trigger) }
            addItem(t(if (trigger.isEnabled) "common.disable" else "common.enable")) {
                trigger.isEnabled = !trigger.isEnabled
                changed()
            }
            addItem(t("common.delete")) { delete(trigger) }
        }

    private fun delete(trigger: Trigger) {
        confirm(t("recording.delete.trigger"), t("trigger.delete.text"), t("common.delete")) {
            triggers().remove(trigger)
            changed()
        }
    }

    private fun changed() {
        markChanged()
        refresh()
    }

    companion object {
        const val ID_GRID = "trigger-grid"
        const val ACTIONS_KEY = "trigger-actions"
    }
}
