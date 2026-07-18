package dev.jvmguard.ui.views.data.transactions

import com.github.mvysny.karibudsl.v10.item
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.badge.Badge
import com.vaadin.flow.component.badge.BadgeVariant
import com.vaadin.flow.component.contextmenu.MenuItem
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridSortOrder
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.splitlayout.SplitLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import dev.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import dev.jvmguard.common.helper.Direction
import dev.jvmguard.connector.api.ServerConnection
import dev.jvmguard.data.transactions.*
import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.components.*
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.findVm
import dev.jvmguard.ui.server.serverTime
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.ui.views.data.VmDataView
import jakarta.annotation.security.PermitAll

@PermitAll
@Route(value = "transactions", layout = MainLayout::class)
@PageTitle("jvmguard: Transactions")
class TransactionsView : VmDataView() {

    private var mode = TransactionMode.CALL_TREE
    private var cursor: TransactionCursor? = null
    private var roots: List<TransactionNode> = emptyList()
    private var maxTime: Long = 0L

    private var drillTarget: TransactionDrill.Target? = null

    private val grid = SelectableTreeGrid<TransactionNode>().apply {
        setSizeFull()
        testId = ID_GRID
        addClassName("jvmguard-transaction-grid")
    }

    private val nameColumn = grid.addComponentHierarchyColumn(::nameCell)
        .setHeader(mode.nameColumnHeader)
        .setFlexGrow(1)
        .setAutoWidth(false)

    private val timeColumn = grid.addComponentColumn(::timeCell)
        .setHeader("Total time")
        .setFlexGrow(0)
        .setWidth("220px")
        .setComparator(compareBy { it.time })

    private val modeTabs = Tabs().apply {
        testId = ID_MODE
        TransactionMode.entries.forEach { add(Tab(it.label)) }
        addSelectedChangeListener { event ->
            if (event.isFromClient) {
                mode = TransactionMode.entries[selectedIndex]
                nameColumn.setHeader(mode.nameColumnHeader)
                applyTimeLineVisibility()
                cursor = null
                reload(resetTimeLine = true)
            }
        }
    }

    private var statusFilter: PolicyType? = null
    private val statusItems = LinkedHashMap<PolicyType?, MenuItem>()

    private val filterOptions = FilterOptionsMenu(ID_STATUS_FILTER) { applyFilterAndRender() }

    private val filterField = TextField().apply {
        placeholder = "Filter by name"
        testId = ID_FILTER
        valueChangeMode = ValueChangeMode.LAZY
        isClearButtonVisible = true
        prefixComponent = VaadinIcon.SEARCH.create()
        suffixComponent = filterOptions.button
        // 50% wider than the default field width.
        setWidth("18em")
        addValueChangeListener { applyFilterAndRender() }
    }

    private val navBar = TransactionNavigationBar(
        serverTime = ::serverTime,
        onPrevious = { moveCursor(Direction.PREVIOUS) },
        onNext = { moveCursor(Direction.NEXT) },
        onShowCurrent = { showCurrent(resetTimeLine = false) },
        onShowTime = ::showTime,
        onIntervalChanged = ::onIntervalChanged,
        onAutoTick = ::autoTick,
    )

    private val exportAnchor = ExportAnchor(ID_EXPORT)

    private val timeLinePanel = TransactionTimeLinePanel(onPointSelected = ::showTime)

    private val message = Span().apply { addClassName("jvmguard-data-message") }

    // Shown on its own line when the cursor's interval is only partly recorded.
    private val infoBadge = Badge("", VaadinIcon.WARNING.create()).apply {
        addThemeVariants(BadgeVariant.WARNING)
        addClassName("jvmguard-transaction-info")
        isVisible = false
    }

    private val gridArea = VerticalLayout().apply {
        setSizeFull()
        isPadding = false
        isSpacing = false
        style.set("padding-block-start", "0.5rem")
    }

    private val split = SplitLayout(gridArea, timeLinePanel).apply {
        orientation = SplitLayout.Orientation.VERTICAL
        setWidthFull()
        style.set("flex", "1 1 0")
        style.set("min-height", "0")
        setSplitterPosition(62.0)
        addClassName("jvmguard-transaction-split")
    }

    init {
        grid.addColumn { Formats.count(it.count) }
            .setHeader("Invocations")
            .setFlexGrow(0)
            .setWidth("130px")
            .setComparator(compareBy { it.count })

        grid.addColumn { Formats.time(it.averageTime) }
            .setHeader("Avg. time")
            .setFlexGrow(0)
            .setWidth("130px")
            .setComparator(compareBy { it.averageTime })

        grid.setSelectionMode(Grid.SelectionMode.SINGLE)
        grid.sort(GridSortOrder.desc(timeColumn).build())
        buildStatusMenu()

        val toolbar = HorizontalLayout().apply {
            addClassName("jvmguard-telemetry-toolbar")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            setWidthFull()
            isWrap = true
            isPadding = false
            add(modeTabs)
            add(NavGroup(*navBar.navigationButtons.toTypedArray()))
            navBar.rangeControls.forEach { add(it) }
            add(filterField)
            add(exportAnchor)
        }
        content.add(toolbar, infoBadge, split)
        content.setFlexGrow(1.0, split)
        applyTimeLineVisibility()
        showGrid()
    }

    private fun applyTimeLineVisibility() {
        val showTimeLine = mode.hasTimeLines
        val splitShown = split.parent.isPresent
        if (showTimeLine && !splitShown) {
            content.replace(gridArea, split)
            split.addToPrimary(gridArea)
            content.setFlexGrow(1.0, split)
        } else if (!showTimeLine && splitShown) {
            split.remove(gridArea)
            content.replace(split, gridArea)
            content.setFlexGrow(1.0, gridArea)
        }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        drillTarget = TransactionDrill.take()
        super.onAttach(attachEvent)
        applyDrillDown()
    }

    override fun onPollTick() {
        if (!anyContextMenuOpen()) {
            navBar.tick()
        }
    }

    override fun onSelectionChanged(selection: VmIdentifier) {
        if (drillTarget != null) {
            return
        }
        cursor = null
        reload(resetTimeLine = true)
    }

    private fun applyDrillDown() {
        val target = drillTarget ?: return
        drillTarget = null
        applyMode(target.mode)
        cursor = null
        showTimeWidening(target.time)
    }

    private fun showTimeWidening(time: Long) {
        withConnection { connection ->
            val vm = cursorVm(connection)
            var candidate: TransactionCursor? = null
            var interval = navBar.selectedInterval
            for (next in TransactionTreeInterval.entries) {
                interval = next
                candidate = connection.getTransactionTreeCursor(vm, next, mode.dataType, time, TimeRequirement.INCLUDED)
                if (candidate.availability.isAvailable) {
                    break
                }
            }
            navBar.selectInterval(interval)
            cursor = candidate
            fetchAndRender(resetTimeLine = false)
        }
    }

    private fun applyMode(newMode: TransactionMode) {
        mode = newMode
        modeTabs.selectedIndex = newMode.ordinal
        nameColumn.setHeader(newMode.nameColumnHeader)
        applyTimeLineVisibility()
    }

    private fun connection(): ServerConnection? = Sessions.current()?.serverConnection

    private fun cursorVm(connection: ServerConnection): VM? = connection.findVm(currentSelection)

    private fun reload(resetTimeLine: Boolean) {
        if (cursor == null) {
            showCurrent(resetTimeLine)
        } else {
            fetchAndRender(resetTimeLine)
        }
    }

    private fun withConnection(block: (ServerConnection) -> Unit) {
        val connection = connection() ?: return showMessage(NOT_CONNECTED)
        block(connection)
    }

    private fun showCurrent(resetTimeLine: Boolean) {
        withConnection { connection ->
            cursor = connection.getCurrentTransactionTreeCursor(cursorVm(connection), navBar.selectedInterval, mode.dataType)
            fetchAndRender(resetTimeLine)
        }
    }

    private fun showTime(time: Long) {
        withConnection { connection ->
            cursor = connection.getTransactionTreeCursor(
                cursorVm(connection), navBar.selectedInterval, mode.dataType, time, TimeRequirement.INCLUDED,
            )
            fetchAndRender(resetTimeLine = false, recenter = false)
        }
    }

    private fun moveCursor(direction: Direction) {
        withConnection { connection ->
            val current = cursor ?: return@withConnection
            val moved = connection.moveTransactionTreeCursor(current, direction)
            cursor = moved
            fetchAndRender(resetTimeLine = false)
            if (moved.gap > 0) {
                Notifications.show("Skipped a data gap of ${gapText(moved.gap)}")
            }
        }
    }

    private fun onIntervalChanged() {
        withConnection { connection ->
            val current = cursor
            if (current == null) {
                showCurrent(resetTimeLine = false)
                return@withConnection
            }
            cursor = connection.changeTransactionCursor(current, cursorVm(connection), navBar.selectedInterval)
            fetchAndRender(resetTimeLine = false)
        }
    }

    private fun autoTick() {
        val connection = connection() ?: return
        val current = cursor ?: return
        val latest = connection.getCurrentTransactionTreeCursor(cursorVm(connection), navBar.selectedInterval, mode.dataType)
        if (latest.startTime > current.startTime) {
            cursor = latest
            fetchAndRender(resetTimeLine = false)
        }
    }

    private fun fetchAndRender(resetTimeLine: Boolean, recenter: Boolean = true) {
        withConnection { connection ->
            val current = cursor
            if (current == null || !current.availability.isAvailable) {
                roots = emptyList()
                // While auto-updating, an empty current interval most likely means data exists earlier
                showMessage(if (current != null && navBar.isAutoUpdate()) NO_CURRENT_DATA_TEXT else NO_DATA_TEXT)
                current?.let(navBar::update)
                if (mode.hasTimeLines) {
                    timeLinePanel.refresh(cursorVm(connection), navBar.selectedInterval, null, resetTimeLine, recenter)
                }
                return@withConnection
            }
            val data = mode.fetch(connection, current)
            roots = TransactionNode.roots(data, mode.cumulateBacktraces)
            updateInfo(data)
            navBar.update(current)
            applyFilterAndRender()
            if (mode.hasTimeLines) {
                timeLinePanel.refresh(cursorVm(connection), navBar.selectedInterval, cursorEndTime(current), resetTimeLine, recenter)
            }
        }
    }

    private fun updateInfo(data: TransactionTreeData) {
        val min = data.minIntervalPercentage
        val max = data.maxIntervalPercentage
        if (min >= 100 && max >= 100) {
            infoBadge.isVisible = false
            return
        }
        val range = if (max == min) "$min" else "$min to $max"
        infoBadge.text =
            "Only $range % of the selected interval has recorded data; values may not be comparable to other intervals."
        infoBadge.isVisible = true
    }

    private fun buildStatusMenu() {
        val menu = filterOptions.menu
        menu.addSeparator()
        // A non-interactive group label
        menu.addItem(Span("Transaction state")).apply {
            addClassName("jvmguard-menu-header")
            isEnabled = false
        }
        val options = linkedMapOf(
            "All" to null,
            "Normal" to PolicyType.NORMAL,
            "Slow" to PolicyType.SLOW,
            "Very slow" to PolicyType.VERY_SLOW,
            "Error" to PolicyType.ERROR,
        )
        options.forEach { (label, type) ->
            val item = menu.addItem(label) { selectStatus(type) }
            item.isCheckable = true
            item.isChecked = type == null
            statusItems[type] = item
        }
    }

    private fun selectStatus(type: PolicyType?) {
        statusFilter = type
        statusItems.forEach { (candidate, item) -> item.isChecked = candidate == type }
        filterOptions.setExtraActive(type != null)
        applyFilterAndRender()
    }

    private fun applyFilterAndRender() {
        val query = filterField.value?.trim().orEmpty()
        val status = statusFilter
        val filtering = query.isNotEmpty() || status != null
        val displayed =
            if (!filtering) {
                roots
            } else {
                roots.mapNotNull { root ->
                    root.filtered { node ->
                        nameMatchesFilter(node.name, query, filterOptions.useRegex, filterOptions.matchCase) && (status == null || node.policyType == status)
                    }
                }
            }
        if (displayed.isEmpty()) {
            showMessage(if (filtering) "No transactions match the current filter." else NO_DATA_TEXT)
            return
        }
        maxTime = displayed.maxOf { it.time }
        grid.setItems(displayed) { it.children }
        showGrid()
        updateExport(displayed)
    }

    private fun cursorEndTime(cursor: TransactionCursor): Long = cursor.startTime + cursor.interval.timeExtent

    private fun timeLineMenu(node: TransactionNode): Component? {
        if (!mode.hasTimeLines || !node.topLevel) {
            return null
        }
        return menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Time line options", ID_ROW_MENU, tooltip = "Show a time line") {
            TransactionTreeValueType.entries.forEach { valueType ->
                item(
                    "Show time line of ${valueType.toString().lowercase()}",
                    { timeLinePanel.showTimeLine(node, valueType) })
            }
        }
    }

    private fun nameCell(node: TransactionNode): Component {
        val name = HorizontalLayout().apply {
            addClassName("jvmguard-transaction-name")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isPadding = false
            isSpacing = false
            node.policyPrefix?.let {
                add(Span(it).apply {
                    addClassName("jvmguard-transaction-policy-prefix")
                    policyClass(node.policyType)?.let { pc -> addClassName(pc) }
                })
            }
            add(Span(node.name).apply { addClassName("jvmguard-transaction-name-text") })
        }
        return HorizontalLayout().apply {
            addClassName("jvmguard-transaction-name-cell")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isPadding = false
            isSpacing = false
            setWidthFull()
            add(name)
            setFlexGrow(1.0, name)
            timeLineMenu(node)?.let { add(it) }
        }
    }

    private fun timeCell(node: TransactionNode): Component {
        val fraction = if (maxTime > 0) node.time.toDouble() / maxTime else 0.0
        val fill = Div().apply {
            addClassName("jvmguard-time-bar-fill")
            style.set("width", "${(fraction * 100).coerceIn(0.0, 100.0)}%")
        }
        val label = Span(Formats.time(node.time)).apply { addClassName("jvmguard-time-bar-label") }
        return Div(fill, label).apply { addClassName("jvmguard-time-bar") }
    }

    private fun updateExport(displayed: List<TransactionNode>) {
        val end = cursor?.let(::cursorEndTime) ?: 0L
        val bytes = TransactionExport.toJson(mode, displayed, navBar.selectedInterval, end)
        val fileName = "transactions-${mode.name.lowercase()}.json"
        exportAnchor.setJsonContent(bytes, fileName)
    }

    private fun showGrid() = gridArea.showFilling(grid)

    private fun showMessage(text: String) {
        exportAnchor.isVisible = false
        infoBadge.isVisible = false
        message.text = text
        gridArea.showCentered(message)
    }

    private fun gapText(gap: Long): String {
        val minutes = gap / 60_000
        return when {
            minutes >= 2 * 24 * 60 -> "${minutes / (24 * 60)} days"
            minutes >= 2 * 60 -> "${minutes / 60} hours"
            else -> "$minutes minutes"
        }
    }

    private fun policyClass(policyType: PolicyType): String? = when (policyType) {
        PolicyType.NORMAL -> null
        PolicyType.SLOW -> "policy-slow"
        PolicyType.VERY_SLOW -> "policy-very-slow"
        PolicyType.PARTIAL -> "policy-partial"
        PolicyType.ERROR -> "policy-error"
    }

    companion object {
        const val ID_GRID = "transaction-grid"
        const val ID_MODE = "transaction-mode"
        const val ID_FILTER = "transaction-filter"
        const val ID_STATUS_FILTER = "transaction-status-filter"
        const val ID_ROW_MENU = "transaction-row-menu"
        const val ID_EXPORT = "transaction-export"

        private const val NOT_CONNECTED = "Not connected to the jvmguard server."
        private const val NO_DATA_TEXT =
            "No recorded transaction data is available for this selection. Try a wider interval or an earlier time."
        private const val NO_CURRENT_DATA_TEXT =
            "No current data has been recorded. Change to previous times to see historical data."
    }
}
