package com.jvmguard.ui.views.data.transactions

import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.transactions.TransactionTreeValueType
import com.jvmguard.data.vmdata.Telemetry
import com.jvmguard.data.vmdata.TelemetryData
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.VM
import com.jvmguard.ui.components.echart.EChart
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.serverTime
import com.jvmguard.ui.views.data.telemetry.TelemetryChartModels
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout

/**
 * The transactions time line is a read-only companion to the transaction grid: its range and
 * resolution are derived from the transaction grid interval, and its time window follows the grid cursor.
 * Clicking a point repositions the grid.
 */
class TransactionTimeLinePanel(
    private val onPointSelected: (Long) -> Unit,
) : VerticalLayout() {

    private sealed interface Mode {
        data object Associated : Mode
        data class Item(val node: TransactionNode, val valueType: TransactionTreeValueType) : Mode
    }

    private var vm: VM? = null
    private var gridInterval: TransactionTreeInterval = TransactionTreeInterval.TEN_MINUTE
    private var gridEndTime: Long? = null
    private var mode: Mode = Mode.Associated

    // The telemetry window's right edge is only re-centered on navigation and load and is kept stable on
    // point-click so scrubbing within the view doesn't shift the chart
    private var telemetryEnd: Long = 0L

    private val title = Span().apply { addClassName("jvmguard-timeline-title") }

    private val caption = Span().apply { addClassName("jvmguard-timeline-range") }

    private val closeButton = Button(VaadinIcon.CLOSE_SMALL.create()) { showAssociated() }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        setAriaLabel("Close time line")
        setTooltipText("Back to the transactions telemetry")
        testId = ID_CLOSE
        isVisible = false
        style.set("margin-inline-start", "auto")
    }

    private val chart = EChart().apply {
        setSizeFull()
        style.set("min-height", "0")
        testId = ID_CHART
        addPointClickListener { event -> onPointSelected(snapToGridInterval(event.time)) }
    }

    private val message = Span().apply { addClassName("jvmguard-data-message") }

    init {
        addClassName("jvmguard-transaction-timeline")
        setWidthFull()
        isPadding = false
        isSpacing = false
        val header = HorizontalLayout().apply {
            addClassName("jvmguard-timeline-header")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            setWidthFull()
            isPadding = false
            isWrap = true
            add(title, caption)
            add(closeButton)
        }
        add(header)
        showChart()
    }

    fun refresh(
        vm: VM?,
        gridInterval: TransactionTreeInterval,
        gridEndTime: Long?,
        resetToAssociated: Boolean,
        recenter: Boolean,
    ) {
        this.vm = vm
        this.gridInterval = gridInterval
        this.gridEndTime = gridEndTime
        if (resetToAssociated) {
            mode = Mode.Associated
        }
        if (recenter || resetToAssociated || telemetryEnd == 0L) {
            val now = serverTime()
            val gridEnd = gridEndTime ?: now
            val window = gridInterval.minimumTimeLineInterval
            telemetryEnd = if (window != null) telemetryEndTime(window, gridEnd, now) else now
        }
        render()
    }

    fun showAssociated() {
        mode = Mode.Associated
        render()
    }

    fun showTimeLine(node: TransactionNode, valueType: TransactionTreeValueType) {
        mode = Mode.Item(node, valueType)
        render()
    }

    private fun render() {
        val connection = Sessions.current()?.serverConnection
        if (connection == null) {
            showMessage("Not connected to the jvmguard server.")
            return
        }
        val window = gridInterval.minimumTimeLineInterval
        if (window == null) {
            showMessage(NO_DATA_TEXT)
            return
        }
        caption.text = window.toString()
        val currentMode = mode
        closeButton.isVisible = currentMode is Mode.Item
        val vm = vm ?: return
        val data = when (currentMode) {
            is Mode.Associated -> {
                title.text = Telemetry.TRANSACTIONS.toString()
                connection.getTelemetryData(vm, Telemetry.TRANSACTIONS.mainId, window, telemetryEnd)
            }

            is Mode.Item -> {
                title.text = "${currentMode.valueType} · ${currentMode.node.name}"
                val treeInterval = window.getTransactionTreeTimeLineInterval() ?: return
                connection.getTransactionTreeTimeLine(
                    vm, telemetryEnd - window.timeExtent, telemetryEnd, currentMode.node.identifier,
                    currentMode.valueType, treeInterval, true,
                )
            }
        }
        renderChart(data, window, telemetryEnd, currentMode is Mode.Associated)
    }

    private fun snapToGridInterval(time: Long): Long = gridInterval.getFloorStartTime(time)

    private fun telemetryEndTime(window: TelemetryInterval, gridEnd: Long, now: Long): Long {
        val gridCenter = gridEnd - gridInterval.timeExtent / 2
        val desiredEnd = gridCenter + window.timeExtent / 2
        return minOf(desiredEnd, now)
    }

    private fun renderChart(
        data: TelemetryData?, window: TelemetryInterval, endTime: Long, isTransactions: Boolean,
    ) {
        val root = data?.rootNode
        if (data == null || root == null || data.isNoPreviousData) {
            showMessage(NO_DATA_TEXT)
            return
        }
        val frequencyUnit = Sessions.current()?.frequencyUnit ?: FrequencyUnit.PER_MINUTE
        val node = if (isTransactions) root.children.firstOrNull { it.data.isNotEmpty() } ?: root else root
        chart.setModel(
            TelemetryChartModels.build(
                telemetryData = data,
                node = node,
                frequencyUnit = frequencyUnit,
                interval = window,
                endTime = endTime,
                isTransactions = isTransactions,
                logarithmic = false,
                frozen = null,
                // The band spans the whole grid interval, the per-item point marker sits at its center
                markTime = gridEndTime?.takeUnless { isTransactions }?.let { it - gridInterval.timeExtent / 2 },
                markBandStart = gridEndTime?.takeIf { isTransactions }?.let { it - gridInterval.timeExtent },
                markBandEnd = gridEndTime?.takeIf { isTransactions },
            )
        )
        showChart()
    }

    private fun showChart() {
        if (chart.parent.isEmpty) {
            remove(message)
            add(chart)
            setFlexGrow(1.0, chart)
        }
    }

    private fun showMessage(text: String) {
        message.text = text
        remove(chart)
        if (message.parent.isEmpty) {
            add(message)
        }
    }

    companion object {
        const val ID_CHART = "transaction-timeline-chart"
        const val ID_CLOSE = "transaction-timeline-close"

        private const val NO_DATA_TEXT = "No time-line data is available for this selection."
    }
}
