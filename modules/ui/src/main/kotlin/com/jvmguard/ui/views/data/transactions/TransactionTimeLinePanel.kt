package com.jvmguard.ui.views.data.transactions

import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.transactions.TransactionTreeValueType
import com.jvmguard.data.vmdata.Telemetry
import com.jvmguard.data.vmdata.TelemetryData
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.VM
import com.jvmguard.ui.components.NavGroup
import com.jvmguard.ui.components.echart.EChart
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.serverTime
import com.jvmguard.ui.views.data.telemetry.TelemetryChartModels
import com.jvmguard.ui.views.data.telemetry.TelemetryNavigationBar
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.shared.Registration

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

    private val navBar = TelemetryNavigationBar(serverTime = ::serverTime, onChange = ::render)

    private val title = Span().apply { addClassName("jvmguard-timeline-title") }

    private val closeButton = Button(VaadinIcon.CLOSE_SMALL.create()) { showAssociated() }.apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        setAriaLabel("Close time line")
        setTooltipText("Back to the transactions telemetry")
        testId = ID_CLOSE
        isVisible = false
        style.set("margin-inline-start", "auto")
    }

    private var pollRegistration: Registration? = null

    private val chart = EChart().apply {
        setSizeFull()
        style.set("min-height", "0")
        testId = ID_CHART
        addPointClickListener { event -> onPointSelected(event.time) }
        addBackwardListener { ctrl -> navBar.previous(ctrl) }
        addForwardListener { ctrl -> navBar.next(ctrl) }
        addZoomInListener { time -> navBar.zoomIn(time) }
    }

    private val message = Span().apply { addClassName("jvmguard-data-message") }

    init {
        addClassName("jvmguard-transaction-timeline")
        setWidthFull()
        isPadding = false
        isSpacing = false
        val navGroup = NavGroup(*navBar.navigationButtons.toTypedArray())
        val header = HorizontalLayout().apply {
            addClassName("jvmguard-timeline-header")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            setWidthFull()
            isPadding = false
            isWrap = true
            add(title, navGroup)
            navBar.rangeControls.forEach { add(it) }
            add(closeButton)
        }
        add(header)
        showChart()
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        pollRegistration = attachEvent.ui.addPollListener { navBar.tick() }
    }

    override fun onDetach(detachEvent: DetachEvent) {
        pollRegistration?.remove()
        pollRegistration = null
        super.onDetach(detachEvent)
    }

    fun refresh(vm: VM?, gridInterval: TransactionTreeInterval, gridEndTime: Long?, resetToAssociated: Boolean) {
        this.vm = vm
        this.gridInterval = gridInterval
        this.gridEndTime = gridEndTime
        if (resetToAssociated) {
            mode = Mode.Associated
            gridInterval.minimumTimeLineInterval?.let { navBar.applyRange(it) }
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
        val currentMode = mode
        closeButton.isVisible = currentMode is Mode.Item
        val animate = navBar.consumeAnimatedNav()
        val window = navBar.selectedInterval
        val endTime = navBar.currentEndTime()
        val vm = vm ?: return
        val data = when (currentMode) {
            is Mode.Associated -> {
                title.text = Telemetry.TRANSACTIONS.toString()
                connection.getTelemetryData(vm, Telemetry.TRANSACTIONS.mainId, window, endTime)
            }

            is Mode.Item -> {
                title.text = "${currentMode.valueType} · ${currentMode.node.name}"
                val treeInterval = window.getTransactionTreeTimeLineInterval() ?: return
                connection.getTransactionTreeTimeLine(
                    vm, endTime - window.timeExtent, endTime, currentMode.node.identifier,
                    currentMode.valueType, treeInterval, true,
                )
            }
        }
        navBar.setPreviousEnabled(!data.isNoPreviousData)
        renderChart(data, window, endTime, currentMode is Mode.Associated, animate)
    }

    private fun renderChart(
        data: TelemetryData?, window: TelemetryInterval, endTime: Long, isTransactions: Boolean, animate: Boolean,
    ) {
        val root = data?.rootNode
        if (data == null || root == null) {
            showMessage(NO_DATA_TEXT)
            return
        }
        val frequencyUnit = Sessions.current()?.frequencyUnit ?: FrequencyUnit.PER_MINUTE
        chart.setModel(
            TelemetryChartModels.build(
                telemetryData = data,
                node = root,
                frequencyUnit = frequencyUnit,
                interval = window,
                endTime = endTime,
                isTransactions = isTransactions,
                logarithmic = false,
                frozen = null,
                canBackward = !data.isNoPreviousData,
                canForward = !navBar.isNowSelected(),
                animate = animate,
                // The band spans the whole interval, the per-item point marker sits at the interval center
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
