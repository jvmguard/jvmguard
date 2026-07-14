package dev.jvmguard.ui.components.recording.thresholds

import dev.jvmguard.common.helper.DeepCopy
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.ui.components.cellRow
import dev.jvmguard.ui.components.editDeleteKeys
import dev.jvmguard.ui.components.enableRowReorder
import dev.jvmguard.ui.components.menuButton
import dev.jvmguard.ui.components.recording.RecordingGrid
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon

class ThresholdGrid(
    private val thresholds: () -> MutableList<Threshold>,
    private val markChanged: () -> Unit,
    private val telemetryTypes: () -> Collection<TelemetryType>,
) : RecordingGrid() {

    // Resolved once per refresh
    private var types: Collection<TelemetryType> = emptyList()

    private val grid = Grid<Threshold>().apply {
        testId = ID_GRID
        addComponentColumn(::nameCell).setHeader("Threshold").setFlexGrow(1)
        addComponentColumn(::rowActions).setKey(ACTIONS_KEY).setAutoWidth(true).setFlexGrow(0)
        setEmptyStateComponent(emptyState("No thresholds yet. Use \"Add threshold\" to create one."))
        addItemDoubleClickListener { edit(it.item) }
        editDeleteKeys(::edit, ::delete)
        enableRowReorder(items = thresholds, onReordered = ::changed)
        isAllRowsVisible = true
    }

    init {
        isPadding = false
        isSpacing = false
        setWidthFull()
        add(grid)
        refresh()
    }

    override fun addNew() {
        val threshold = Threshold()
        ThresholdDialog(threshold, isNew = true, types, thresholds().toList()) {
            thresholds().add(it)
            changed()
        }.open()
    }

    override fun refresh() {
        types = telemetryTypes()
        grid.setItems(thresholds())
    }

    private fun edit(threshold: Threshold) {
        ThresholdDialog(DeepCopy.clone(threshold), isNew = false, types, thresholds().filter { it !== threshold }) { saved ->
            val index = thresholds().indexOf(threshold)
            if (index >= 0) {
                thresholds()[index] = saved
            }
            changed()
        }.open()
    }

    private fun nameCell(threshold: Threshold): Component {
        val name = Span(displayName(threshold))
        val detail = Span(boundsText(threshold)).apply { addClassName("jvmguard-row-detail") }
        return cellRow(name, detail)
    }

    private fun displayName(threshold: Threshold): String = thresholdDisplayName(threshold, types)

    private fun boundsText(threshold: Threshold): String {
        val labels = telemetryTypeOf(threshold.telemetryIdentifier, types)?.unit?.labels
        fun unit(level: Int) = labels?.getOrNull(level)?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        val parts = buildList {
            if (threshold.isLowerBoundEnabled) add("≥ ${threshold.lowerBound}${unit(threshold.lowerBoundUnitLevel)}")
            if (threshold.isUpperBoundEnabled) add("≤ ${threshold.upperBound}${unit(threshold.upperBoundUnitLevel)}")
        }
        return if (parts.isEmpty()) "" else parts.joinToString(", ", prefix = "[", postfix = "]")
    }

    private fun rowActions(threshold: Threshold): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions", "threshold-row-menu-${displayName(threshold)}") {
            addItem("Edit") { edit(threshold) }
            addItem("Delete") { delete(threshold) }
        }

    private fun delete(threshold: Threshold) {
        confirmDelete("threshold", displayName(threshold)) {
            thresholds().remove(threshold)
            changed()
        }
    }

    private fun changed() {
        markChanged()
        refresh()
    }

    companion object {
        const val ID_GRID = "threshold-grid"
        const val ACTIONS_KEY = "threshold-actions"
    }
}
