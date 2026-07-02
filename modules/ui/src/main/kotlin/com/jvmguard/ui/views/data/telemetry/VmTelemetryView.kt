package com.jvmguard.ui.views.data.telemetry

import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.vmdata.*
import com.jvmguard.ui.components.ExportAnchor
import com.jvmguard.ui.components.NavGroup
import com.jvmguard.ui.components.echart.EChart
import com.jvmguard.ui.components.echart.TelemetryChartModel
import com.jvmguard.ui.components.showCentered
import com.jvmguard.ui.components.showFilling
import com.jvmguard.ui.components.sparkline.Sparkline
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.findVm
import com.jvmguard.ui.server.serverTime
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.data.VmDataView
import com.jvmguard.ui.views.data.transactions.TransactionDrill
import com.jvmguard.ui.views.data.transactions.TransactionMode
import com.jvmguard.ui.views.data.transactions.TransactionsView
import com.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.contextmenu.MenuItem
import com.vaadin.flow.component.dependency.Uses
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route(value = "telemetry", layout = MainLayout::class)
@PageTitle("jvmguard: Telemetry")
@Uses(Sparkline::class)
class VmTelemetryView : VmDataView() {

    private enum class ViewMode(val label: String) {
        TELEMETRY("Telemetry"),
        OVERVIEW("Overview"),
    }

    private var viewMode = ViewMode.TELEMETRY

    private var sources: List<TelemetrySource> = emptyList()
    private var selectedSource: TelemetrySource? = null
    private var selectedSourceKey: String? = null
    private var selectedNodeDescription: String? = null

    // Selection carried over from a sparkline cell
    private var pendingMainId: String? = null
    private var pendingRange: String? = null
    private var pendingSubId: String? = null
    private var pendingOverview = false

    private var telemetryData: TelemetryData? = null
    private var endTime: Long = 0L
    private var nodeDepths: Map<TelemetryNode, Int> = emptyMap()
    private var logarithmic = false
    private var frozen: YRange? = null
    private var lastModel: TelemetryChartModel? = null

    private val navBar = TelemetryNavigationBar(serverTime = ::serverTime, onChange = ::reload)

    private val chart = EChart().apply {
        setSizeFull()
        testId = ID_CHART
        addZoomInListener { time -> navBar.zoomIn(time) }
        addBackwardListener { ctrl -> navBar.previous(ctrl) }
        addForwardListener { ctrl -> navBar.next(ctrl) }
        addPointClickListener { openTransactions(it.time, TransactionMode.CALL_TREE) }
    }

    private val message = Span().apply { addClassName("jvmguard-data-message") }

    private val chartArea = VerticalLayout().apply {
        setSizeFull()
        isPadding = false
        isSpacing = false
    }

    private val typeSelect = Select<TelemetrySource>().apply {
        label = "Telemetry"
        testId = ID_TYPE_SELECT
        setItemLabelGenerator { it.label }
        addValueChangeListener { event ->
            if (event.isFromClient && event.value != null) {
                selectedSource = event.value
                selectedSourceKey = event.value.key
                selectedNodeDescription = null
                reload()
            }
        }
    }

    private val nodeSelect = Select<TelemetryNode>().apply {
        label = "Series"
        testId = ID_NODE_SELECT
        isVisible = false
        addValueChangeListener { event ->
            if (event.isFromClient && event.value != null) {
                selectedNodeDescription = event.value.description
                renderNode(event.value)
            }
        }
    }

    private val exportAnchor = ExportAnchor(ID_EXPORT)

    private lateinit var zoomInItem: MenuItem
    private lateinit var zoomOutItem: MenuItem

    private val modeTabs = Tabs().apply {
        testId = ID_MODE
        ViewMode.entries.forEach { add(Tab(it.label)) }
        addSelectedChangeListener {
            val newMode = ViewMode.entries[selectedIndex]
            if (newMode != viewMode) {
                applyViewMode(newMode)
            }
        }
    }

    private val overviewPanel = TelemetryOverviewPanel { currentSelection }

    private val telemetryControls = HorizontalLayout().apply {
        addClassName("jvmguard-telemetry-toolbar")
        defaultVerticalComponentAlignment = FlexComponent.Alignment.END
        isWrap = true
        isPadding = false
        add(NavGroup(*navBar.navigationButtons.toTypedArray()))
        add(typeSelect, nodeSelect)
        navBar.rangeControls.forEach { add(it) }
        add(exportAnchor)
    }

    init {
        val toolbar = HorizontalLayout().apply {
            addClassName("jvmguard-telemetry-toolbar")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            setWidthFull()
            isWrap = true
            isPadding = false
            add(modeTabs, telemetryControls)
            setFlexGrow(1.0, telemetryControls)
        }
        content.add(toolbar, chartArea)
        content.setFlexGrow(1.0, chartArea)
        setupContextMenu()
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        applyPendingMode()
    }

    override fun onPollTick() {
        if (viewMode == ViewMode.TELEMETRY) {
            navBar.tick()
        }
    }

    private fun applyPendingMode() {
        when {
            pendingOverview -> {
                pendingOverview = false
                // Overview ignores any stale chart command carried over on the cached instance.
                pendingMainId = null
                pendingRange = null
                pendingSubId = null
                if (!selectionRendered) {
                    applyViewMode(ViewMode.OVERVIEW, load = true)
                }
            }

            hasPendingCommand() && !selectionRendered -> applyViewMode(ViewMode.TELEMETRY, load = true)
        }
    }

    private fun applyViewMode(mode: ViewMode, load: Boolean = true) {
        viewMode = mode
        if (modeTabs.selectedIndex != mode.ordinal) {
            modeTabs.selectedIndex = mode.ordinal
        }
        // Reserve the control height in overview mode rather than removing
        // it, so the toolbar keeps the same height and the mode tabs don't jump
        telemetryControls.element.classList.set(HIDDEN_CONTROLS_CLASS, mode != ViewMode.TELEMETRY)
        when (mode) {
            ViewMode.TELEMETRY -> {
                content.remove(overviewPanel)
                content.add(chartArea)
                content.setFlexGrow(1.0, chartArea)
                if (load) {
                    reload()
                }
            }

            ViewMode.OVERVIEW -> {
                content.remove(chartArea)
                content.add(overviewPanel)
                content.setFlexGrow(1.0, overviewPanel)
                if (load) {
                    overviewPanel.reload()
                }
            }
        }
    }

    private fun setupContextMenu() {
        val menu = ContextMenu()
        menu.target = chart
        menu.addItem("Freeze Y-axis") { event -> onFreezeToggle(event.source.isChecked) }.isCheckable = true
        menu.addItem("Logarithmic Y-axis") { event ->
            logarithmic = event.source.isChecked
            renderCurrent()
        }.isCheckable = true
        zoomInItem = menu.addItem("Zoom in here") { navBar.zoomIn(contextTime()) }
        zoomOutItem = menu.addItem("Zoom out here") { navBar.zoomOut(contextTime()) }
        menu.addItem("Show call tree") { contextTime()?.let { openTransactions(it, TransactionMode.CALL_TREE) } }
        menu.addItem("Show hot spots") { contextTime()?.let { openTransactions(it, TransactionMode.HOT_SPOTS) } }
    }

    private fun openTransactions(time: Long, mode: TransactionMode) {
        Sessions.vmSelectionModel().set(currentSelection)
        TransactionDrill.set(time, mode)
        UI.getCurrent().navigate(TransactionsView::class.java)
    }

    private fun contextTime(): Long? = chart.lastContextTime.takeIf { it > 0L }

    private fun updateZoomItemsEnabled() {
        zoomInItem.isEnabled = navBar.isZoomInEnabled()
        zoomOutItem.isEnabled = navBar.isZoomOutEnabled()
    }

    private fun onFreezeToggle(checked: Boolean) {
        frozen = if (checked) frozenRange() else null
        renderCurrent()
    }

    private fun frozenRange(): YRange? {
        val min = chart.lastYMin
        val max = chart.lastYMax
        if (min.isFinite() && max.isFinite()) {
            return YRange(min, max)
        }
        return lastModel?.let(::computeFrozenRange)
    }

    private fun computeFrozenRange(model: TelemetryChartModel): YRange {
        val maxY = if (model.stacked) {
            model.series
                .flatMap { it.points }
                .groupBy { it.t }
                .values
                .maxOfOrNull { points -> points.sumOf { it.v ?: 0.0 } }
        } else {
            model.series.flatMap { it.points }.mapNotNull { it.v }.maxOrNull()
        }
        return YRange(min = if (model.zeroBase) 0.0 else null, max = maxY?.times(1.05))
    }

    private fun renderCurrent() {
        currentNode()?.let(::renderNode)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        // Carry over the telemetry type / range / subseries when opened from a sparkline cell.
        val params = event.location.queryParameters.parameters
        params[PARAM_TYPE]?.firstOrNull()?.let { pendingMainId = it }
        params[PARAM_RANGE]?.firstOrNull()?.let { pendingRange = it }
        params[PARAM_SUB]?.firstOrNull()?.let { pendingSubId = it }
        pendingOverview = params[PARAM_MODE]?.firstOrNull() == MODE_OVERVIEW
        if (pendingOverview && viewMode != ViewMode.OVERVIEW) {
            applyViewMode(ViewMode.OVERVIEW, load = false)
        } else if (pendingMainId != null && viewMode == ViewMode.OVERVIEW) {
            applyViewMode(ViewMode.TELEMETRY, load = false)
        }
        super.beforeEnter(event)
        if (isAttached) {
            applyPendingMode()
        }
    }

    private fun hasPendingCommand(): Boolean =
        pendingMainId != null || pendingRange != null || pendingSubId != null

    override fun onSelectionChanged(selection: VmIdentifier) {
        when (viewMode) {
            ViewMode.TELEMETRY -> reload()
            ViewMode.OVERVIEW -> overviewPanel.reload()
        }
    }

    private fun reload() {
        val animate = navBar.consumeAnimatedNav()
        val connection = Sessions.current()?.serverConnection
        if (connection == null) {
            showMessage("Not connected to the jvmguard server.")
            return
        }
        ensureSources(connection)
        applyPendingNav()
        val source = selectedSource
        val vm = connection.findVm(currentSelection)
        if (source == null || vm == null) {
            showMessage(NO_DATA_TEXT)
            return
        }
        endTime = navBar.currentEndTime()
        val data = source.fetch(connection, vm, navBar.selectedInterval, endTime)
        telemetryData = data
        navBar.setPreviousEnabled(data != null && !data.isNoPreviousData)
        val rootNode = data?.rootNode
        if (data == null || data.isNoPreviousData || rootNode == null) {
            showMessage(NO_DATA_TEXT)
            return
        }
        applyPendingSubId(rootNode)
        populateNodeSelect(rootNode)
        val node = currentNode()
        if (node == null) {
            showMessage(NO_DATA_TEXT)
        } else {
            renderNode(node, animate = animate)
        }
    }

    private fun ensureSources(connection: ServerConnection) {
        if (sources.isNotEmpty()) {
            return
        }
        sources = TelemetrySources.build(connection)
        typeSelect.setItems(sources)
        selectedSource = sources.firstOrNull { it.key == selectedSourceKey }
            ?: TelemetrySources.byMainId(sources, Telemetry.CPU.mainId)
                    ?: sources.firstOrNull()
        selectedSourceKey = selectedSource?.key
        typeSelect.value = selectedSource
    }

    private fun applyPendingNav() {
        pendingMainId?.let { mainId ->
            pendingMainId = null
            TelemetrySources.byMainId(sources, mainId)?.let {
                selectedSource = it
                selectedSourceKey = it.key
                typeSelect.value = it
                selectedNodeDescription = null
            }
        }
        pendingRange?.let { range ->
            pendingRange = null
            rangeToInterval(range)?.let { navBar.applyRange(it) }
        }
    }

    private fun rangeToInterval(range: String): TelemetryInterval? = when (range) {
        SparkLineRange.LAST_HOUR.name -> TelemetryInterval.EIGHTY_MINUTES
        SparkLineRange.LAST_DAY.name -> TelemetryInterval.ONE_DAY
        else -> null
    }

    private fun applyPendingSubId(root: TelemetryNode) {
        val subId = pendingSubId ?: return
        pendingSubId = null
        val match = flattenNodes(root).map { it.first }.firstOrNull { node -> node.data.any { it.subId == subId } }
        if (match != null) {
            selectedNodeDescription = match.description
        }
    }

    private fun renderNode(node: TelemetryNode, animate: Boolean = false) {
        val data = telemetryData ?: return
        val frequencyUnit = Sessions.current()?.frequencyUnit ?: FrequencyUnit.PER_MINUTE
        val model = TelemetryChartModels.build(
            telemetryData = data,
            node = node,
            frequencyUnit = frequencyUnit,
            interval = navBar.selectedInterval,
            endTime = endTime,
            isTransactions = selectedSource?.isTransactions ?: false,
            logarithmic = logarithmic,
            frozen = frozen,
            canBackward = data.isNoPreviousData.not(),
            canForward = navBar.isNowSelected().not(),
            animate = animate,
        )
        lastModel = model
        chart.setModel(model)
        updateZoomItemsEnabled()
        updateExport(node)
        showChart()
    }

    private fun updateExport(node: TelemetryNode) {
        val data = telemetryData ?: return
        val bytes = TelemetryExport.toJson(data, node, navBar.selectedInterval, endTime)
        val name = (selectedSource?.exportId ?: "telemetry").lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val fileName = "telemetry-$name.json"
        exportAnchor.setJsonContent(bytes, fileName)
    }

    private fun populateNodeSelect(root: TelemetryNode) {
        val flat = flattenNodes(root)
        nodeDepths = flat.toMap()
        val nodes = flat.map { it.first }
        nodeSelect.setItems(nodes)
        nodeSelect.setItemLabelGenerator { "--".repeat(nodeDepths[it] ?: 0) + it.description }
        nodeSelect.value = nodes.firstOrNull { it.description == selectedNodeDescription } ?: nodes.firstOrNull()
        nodeSelect.isVisible = nodes.size > 1
    }

    private fun currentNode(): TelemetryNode? {
        val nodes = nodeDepths.keys
        return nodes.firstOrNull { it.description == selectedNodeDescription } ?: nodes.firstOrNull()
    }

    private fun flattenNodes(root: TelemetryNode): List<Pair<TelemetryNode, Int>> {
        val result = ArrayList<Pair<TelemetryNode, Int>>()
        fun visit(node: TelemetryNode, depth: Int) {
            var childDepth = depth
            if (node.description.isNotEmpty()) {
                result.add(node to depth)
                childDepth = depth + 1
            }
            node.children.forEach { visit(it, childDepth) }
        }
        visit(root, 0)
        return result
    }

    private fun showChart() = chartArea.showFilling(chart)

    private fun showMessage(text: String) {
        exportAnchor.isVisible = false
        message.text = text
        chartArea.showCentered(message)
    }

    companion object {
        private const val HIDDEN_CONTROLS_CLASS = "jvmguard-controls-hidden"

        const val ID_CHART = "telemetry-chart"
        const val ID_MODE = "telemetry-mode"
        const val ID_TYPE_SELECT = "telemetry-type-select"
        const val ID_NODE_SELECT = "telemetry-node-select"
        const val ID_EXPORT = "telemetry-export"

        // Query params carried from a sparkline cell
        const val PARAM_TYPE = "t"
        const val PARAM_RANGE = "range"
        const val PARAM_SUB = "sub"

        const val PARAM_MODE = "view"
        const val MODE_OVERVIEW = "overview"

        private const val NO_DATA_TEXT =
            "There is no previously recorded data. Please try a wider time range."
    }
}
