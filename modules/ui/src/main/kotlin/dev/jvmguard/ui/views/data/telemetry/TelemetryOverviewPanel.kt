package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.common.notification.ModificationType
import dev.jvmguard.data.vmdata.*
import dev.jvmguard.ui.components.showCentered
import dev.jvmguard.ui.components.showFilling
import dev.jvmguard.ui.components.sparkline.SparklineRenderers
import dev.jvmguard.ui.components.sparkline.SparklineState
import dev.jvmguard.ui.server.ModificationListener
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.findVm
import dev.jvmguard.ui.server.registerModificationListener
import dev.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout

class TelemetryOverviewPanel(private val selectionProvider: () -> VmIdentifier) :
    VerticalLayout(), ModificationListener {

    private val grid = Grid<TelemetryOverviewRow>()
    private val message = Span().apply { addClassName("jvmguard-data-message") }

    private val lastDayColumn: Grid.Column<TelemetryOverviewRow>
    private val lastHourColumn: Grid.Column<TelemetryOverviewRow>

    private var focusedRow: TelemetryOverviewRow? = null
    private var focusedColumn: Grid.Column<TelemetryOverviewRow>? = null

    init {
        setSizeFull()
        isPadding = false
        isSpacing = false
        // Room between the toolbar and the grid header
        style.set("padding-block-start", "0.5rem")

        grid.setSizeFull()
        grid.testId = ID_GRID
        grid.addClassName("jvmguard-telemetry-overview-grid")

        grid.addColumn { it.name }
            .setHeader("Telemetry").setFlexGrow(1).setWidth("280px")

        lastDayColumn = sparklineColumn("Last day", SparkLineRange.LAST_DAY) { it.lastDayState() }
        lastHourColumn = sparklineColumn("Last hour", SparkLineRange.LAST_HOUR) { it.lastHourState() }

        grid.addColumn(SparklineRenderers.display<TelemetryOverviewRow> { it.currentState() })
            .setHeader("Current").setFlexGrow(0).setWidth(CURRENT_COLUMN_WIDTH)

        grid.addCellFocusListener { event ->
            focusedRow = event.item.orElse(null)
            focusedColumn = event.column.orElse(null)
        }
        // Activate the focused sparkline cell with Space
        grid.element.addEventListener("keydown") { activateFocusedCell() }
            .setFilter("event.key === ' ' && event.target === event.currentTarget")
            .addEventData("event.preventDefault()")
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        val session = Sessions.current() ?: return
        registerModificationListener(session)
        addDetachListener { it.unregisterListener() }
    }

    override fun modifyNotified(modificationTypes: Set<ModificationType>) = reload()

    private fun sparklineColumn(
        header: String, range: SparkLineRange, state: (TelemetryOverviewRow) -> SparklineState,
    ): Grid.Column<TelemetryOverviewRow> =
        grid.addColumn(
            SparklineRenderers.column<TelemetryOverviewRow>(
                { state(it) },
                { TelemetryNavigation.open(selectionProvider(), it.telemetryType, range) })
        )
            .setHeader(header).setFlexGrow(0).setWidth(SPARKLINE_COLUMN_WIDTH)

    private fun activateFocusedCell() {
        val row = focusedRow ?: return
        val range = when (focusedColumn) {
            lastDayColumn -> SparkLineRange.LAST_DAY
            lastHourColumn -> SparkLineRange.LAST_HOUR
            else -> return
        }
        TelemetryNavigation.open(selectionProvider(), row.telemetryType, range)
    }

    fun reload() {
        val connection = Sessions.current()?.serverConnection
        if (connection == null) {
            showMessage("Not connected to the jvmguard server.")
            return
        }
        val types = displayedTelemetryTypes(connection)
        if (types.isEmpty()) {
            showMessage(NO_DATA_TEXT)
            return
        }
        val selection = selectionProvider()
        val lastHour = holderFor(connection, selection, SparkLineRange.LAST_HOUR, types)
        val lastDay = holderFor(connection, selection, SparkLineRange.LAST_DAY, types)
        if (lastHour == null && lastDay == null) {
            showMessage(NO_DATA_TEXT)
            return
        }
        grid.setItems(types.map {
            TelemetryOverviewRow(it, lastHour?.getSparkLineData(it), lastDay?.getSparkLineData(it))
        })
        showGrid()
    }

    private fun displayedTelemetryTypes(connection: ServerConnection): List<TelemetryType> =
        connection.idToTelemetryType.values
            .filter { it.isVisible }
            .sortedWith(TELEMETRY_ORDER)

    private fun holderFor(
        connection: ServerConnection, selection: VmIdentifier, range: SparkLineRange, types: List<TelemetryType>,
    ): VmDataHolder? =
        if (selection.isRoot || selection.type.isGroupNode) {
            connection.getGroupVmDataHolder(selection, range, types)
        } else {
            connection.findVm(selection)?.let { connection.getVmDataHolder(it, range, types) }
        }

    private fun showGrid() = showFilling(grid)

    private fun showMessage(text: String) {
        message.text = text
        showCentered(message)
    }

    companion object {
        const val ID_GRID = "telemetry-overview-grid"

        private const val SPARKLINE_COLUMN_WIDTH = "190px"
        private const val CURRENT_COLUMN_WIDTH = "130px"
        private const val NO_DATA_TEXT = "There are no telemetries to display for this selection."

        private val TELEMETRY_ORDER: Comparator<TelemetryType> = compareBy(
            { Telemetry.getByMainId(it.telemetryIdentifier.mainId)?.ordinal ?: Int.MAX_VALUE },
            { it.name },
        )
    }
}
