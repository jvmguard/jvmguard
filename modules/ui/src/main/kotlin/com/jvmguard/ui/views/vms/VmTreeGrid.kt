package com.jvmguard.ui.views.vms

import com.github.mvysny.karibudsl.v10.icon
import com.github.mvysny.karibudsl.v10.item
import com.github.mvysny.karibudsl.v10.span
import com.jvmguard.agent.config.VmType
import com.jvmguard.data.config.triggers.actions.RecordJfrAction
import com.jvmguard.data.config.triggers.actions.RecordJpsAction
import com.jvmguard.data.dashboard.Group
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.viewsettings.SparkLineScaleMode
import com.jvmguard.data.vmdata.*
import com.jvmguard.ui.components.*
import com.jvmguard.ui.components.recording.triggers.TriggerActionDialog
import com.jvmguard.ui.components.sparkline.Sparkline
import com.jvmguard.ui.components.sparkline.SparklineRenderers
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.runInBackground
import com.jvmguard.ui.views.data.mbeans.MBeansView
import com.jvmguard.ui.views.data.telemetry.TelemetryNavigation
import com.jvmguard.ui.views.data.transactions.TransactionsView
import com.jvmguard.connector.api.ServerConnection
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dependency.Uses
import com.vaadin.flow.component.grid.GridSortOrder
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.data.provider.SortDirection
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider.HierarchyFormat
import com.vaadin.flow.data.provider.hierarchy.TreeData
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider

@Uses(Sparkline::class)
class VmTreeGrid : SelectableTreeGrid<VmTreeItem>() {

    private val treeData = TreeData<VmTreeItem>()
    private val dataProvider = TreeDataProvider(treeData, HierarchyFormat.FLATTENED)
    private val sparklineColumns = LinkedHashMap<TelemetryType, Column<VmTreeItem>>()
    private val nameColumn: Column<VmTreeItem>

    private var visibleTelemetryTypes: List<TelemetryType> = emptyList()
    private var focusedItem: VmTreeItem? = null
    private var focusedColumn: Column<VmTreeItem>? = null
    private var lastSortColumn: Column<VmTreeItem>
    private var lastSortDirection = SortDirection.ASCENDING
    private var defaultExpansionApplied = false
    private var canProfile: Boolean? = null
    private var currentTime = 0L
    private var currentRange = SparkLineRange.LAST_HOUR

    init {
        setWidthFull()
        minHeight = "0"
        testId = ID_GRID
        addClassName("jvmguard-vm-grid")
        style.set("user-select", "none").set("-webkit-user-select", "none")
        setDataProvider(dataProvider)

        nameColumn = addComponentHierarchyColumn { nameComponent(it) }
            .setHeader("Name").setFlexGrow(1).setWidth("320px")
            .setComparator(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        addComponentColumn { statusComponent(it) }.setHeader("Status").setFlexGrow(0).setWidth("140px")
            .setComparator(compareBy { it.statusSortValue() })
        sort(GridSortOrder.asc(nameColumn).build())
        lastSortColumn = nameColumn

        // Drive sorting manually: each column is a two-state toggle (never the grid's "none" step), and
        // the first click sorts ascending for Name but descending for the numeric columns. The grid's
        // built-in asc → desc → none cycle can't express either, so the event is used only as "the user
        // clicked a sortable header" and we re-apply our own (column, direction).
        addSortListener { event ->
            if (!event.isFromClient) return@addSortListener
            val sortOrder = event.sortOrder
            val clicked = if (sortOrder.isEmpty()) lastSortColumn else sortOrder.first().sorted
            val direction = when (clicked) {
                lastSortColumn ->
                    if (lastSortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING
                    else SortDirection.ASCENDING

                nameColumn -> SortDirection.ASCENDING
                else -> SortDirection.DESCENDING
            }
            lastSortColumn = clicked
            lastSortDirection = direction
            sort(listOf(GridSortOrder(clicked, direction)))
        }

        addCellFocusListener { event ->
            focusedItem = event.item.orElse(null)
            focusedColumn = event.column.orElse(null)
        }
        // Activate the focused cell's link (name or sparkline) with Space, while the cell is focused but
        // not "entered" (interactive mode). The filter fires only when the keydown target is the grid
        // itself — a focused shadow-DOM grid cell retargets to the host, whereas a focused inner
        // component (e.g. the actions menu) does not — so it doesn't hijack Space inside components.
        element.addEventListener("keydown") { activateFocusedCell() }
            .setFilter("event.key === ' ' && event.target === event.currentTarget")
            .addEventData("event.preventDefault()")
    }

    fun setVisibleTelemetryTypes(telemetryTypes: List<TelemetryType>) {
        visibleTelemetryTypes = telemetryTypes
        sparklineColumns.values.forEach { removeColumn(it) }
        sparklineColumns.clear()
        for (telemetryType in telemetryTypes) {
            val column = addColumn(
                SparklineRenderers.column<VmTreeItem>(
                    { it.sparklineState(telemetryType) },
                    { openTelemetry(it, telemetryType) })
            )
            column.setHeader(telemetryType.name).setFlexGrow(0).setWidth(SPARKLINE_COLUMN_WIDTH)
                .setComparator(compareBy { it.currentValue(telemetryType) })
            sparklineColumns[telemetryType] = column
        }
    }

    fun reload(filter: VmFilter, range: SparkLineRange, scaleMode: SparkLineScaleMode) {
        val connection = Sessions.current()?.serverConnection ?: return
        currentRange = range
        currentTime = connection.currentTime
        val roots = buildRoots(connection.getVmDataHolders(filter, range, visibleTelemetryTypes))
        applyScaleMode(scaleMode, roots)
        treeData.clear()
        treeData.addItems(roots) { it.children }
        dataProvider.refreshAll()
        if (!defaultExpansionApplied) {
            defaultExpansionApplied = true
            applyDefaultExpansion(roots)
        }
    }

    private fun activateFocusedCell() {
        val item = focusedItem ?: return
        when {
            focusedColumn == nameColumn -> showTransactions(item)
            else -> sparklineColumns.entries.firstOrNull { it.value == focusedColumn }
                ?.let { openTelemetry(item, it.key) }
        }
    }

    private fun applyDefaultExpansion(roots: List<VmTreeItem>) {
        roots.minWithOrNull(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })?.let { expand(it) }
        expandSingleChildChains(roots)
    }

    private fun expandSingleChildChains(siblings: List<VmTreeItem>) {
        if (siblings.size == 1) {
            val only = siblings.first()
            expand(only)
            expandSingleChildChains(only.children)
        }
    }

    private fun buildRoots(root: Group<VmDataHolder>): List<VmTreeItem> {
        val result = mutableListOf<VmTreeItem>()
        root.groupChildren.forEach { (identifier, child) -> result.add(buildGroup(identifier, child, "")) }
        root.vmDataMap.values.forEach { result.add(SingleVmItem(it)) }
        return result
    }

    private fun buildGroup(identifier: VmIdentifier, group: Group<VmDataHolder>, parentKey: String): VmGroupItem {
        val name = displayName(identifier)
        val key = if (parentKey.isEmpty()) name else "$parentKey/$name"
        val item = VmGroupItem(key, name, identifier.type, group.data)
        group.groupChildren.forEach { (childId, childGroup) -> item.addChild(buildGroup(childId, childGroup, key)) }
        group.vmDataMap.values.forEach { item.addChild(SingleVmItem(it)) }
        return item
    }

    private fun applyScaleMode(scaleMode: SparkLineScaleMode, roots: List<VmTreeItem>) {
        if (scaleMode == SparkLineScaleMode.SEPARATE) {
            return
        }
        val all = mutableListOf<VmTreeItem>()
        flatten(roots, all)
        if (scaleMode == SparkLineScaleMode.COMMON) {
            for (telemetryType in visibleTelemetryTypes) {
                val max = all.maxOfOrNull { it.getScaledMax(telemetryType) } ?: 0.0
                all.forEach { it.setGraphMax(telemetryType, max) }
            }
            return
        }
        assignSiblingMax(roots)
        all.filter { it.children.isNotEmpty() }.forEach { assignSiblingMax(it.children) }
    }

    private fun assignSiblingMax(siblings: List<VmTreeItem>) {
        for (telemetryType in visibleTelemetryTypes) {
            val max = siblings.maxOfOrNull { it.getScaledMax(telemetryType) } ?: 0.0
            siblings.forEach { it.setGraphMax(telemetryType, max) }
        }
    }

    private fun flatten(items: List<VmTreeItem>, target: MutableList<VmTreeItem>) {
        for (item in items) {
            target.add(item)
            flatten(item.children, target)
        }
    }

    private fun nameComponent(item: VmTreeItem): Component = cellRow().apply {
        if (item is VmGroupItem) {
            icon(vmTypeIcon(item.vmType)) { setSize(GRID_ICON_SIZE) }
            span(item.name)
            add(showMenu(item))
            return@apply
        }
        val vm = item as SingleVmItem
        icon(vmTypeIcon(vm.vmType)) { setSize(GRID_ICON_SIZE) }
        span(vm.name)
        vm.secondaryText?.let { detail -> span(detail) { addClassName("jvmguard-vm-detail") } }
        if (vm.isOutdatedAgent) {
            icon(VaadinIcon.WARNING) {
                setSize(GRID_ICON_SIZE)
                style.set("color", "var(--jvmguard-status-warning)")
                setTooltipText("The jvmguard agent on this VM is outdated; it updates when the VM restarts.")
            }
        }
        // The show + actions triggers form one tight cluster (no gap between them).
        add(cellRow(gap = "0").apply {
            add(showMenu(item))
            if (canProfile() && vm.isConnected) {
                add(actionsMenu(vm.vm))
            }
        })
    }

    private fun statusComponent(item: VmTreeItem): Component = cellRow().apply {
        if (item is VmGroupItem) {
            span(item.vmCount.toString()) { style.set("font-weight", "bold") }
            span("JVMs")
            return@apply
        }
        val vm = item as SingleVmItem
        icon(VaadinIcon.CIRCLE) {
            setSize("0.9em")
            style.set("color", if (vm.isConnected) "var(--jvmguard-status-ok)" else "var(--jvmguard-status-disabled)")
        }
        span("since " + formatSince(currentTime - vm.statusChangeTime))
    }

    private fun openTelemetry(item: VmTreeItem, telemetryType: TelemetryType) =
        TelemetryNavigation.open(item.selectionId, telemetryType, currentRange)

    private fun showMenu(item: VmTreeItem): Component =
        menuButton(VaadinIcon.ANGLE_RIGHT, "Show", ID_SHOW) {
            item("Transactions", { showTransactions(item) }) { testId = ID_SHOW_TRANSACTIONS }
            item("Telemetries", { showTelemetries(item) }) { testId = ID_SHOW_TELEMETRIES }
            if (canBrowseMBeans(item)) {
                item("MBeans", { showMBeans(item) }) { testId = ID_SHOW_MBEANS }
            }
        }

    private fun showTransactions(item: VmTreeItem) {
        Sessions.vmSelectionModel().set(item.selectionId)
        UI.getCurrent().navigate(TransactionsView::class.java)
    }

    private fun showTelemetries(item: VmTreeItem) = TelemetryNavigation.openOverview(item.selectionId)

    private fun showMBeans(item: VmTreeItem) {
        Sessions.vmSelectionModel().set(item.selectionId)
        UI.getCurrent().navigate(MBeansView::class.java)
    }

    private fun canBrowseMBeans(item: VmTreeItem): Boolean = when (item) {
        is SingleVmItem -> true
        is VmGroupItem -> item.vmType == VmType.POOL
        else -> false
    }

    private fun actionsMenu(vm: VM): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions", ID_ACTIONS) {
            item("Run GC", { runGc(vm) }) { testId = ID_ACTION_GC }
            item("Heap dump…", { confirmHeapDump(vm) }) { testId = ID_ACTION_HEAP_DUMP }
            item("Thread dump…", { confirmThreadDump(vm) }) { testId = ID_ACTION_THREAD_DUMP }
            item("Record JProfiler snapshot…", { recordJps(vm) }) { testId = ID_ACTION_RECORD_JPS }
            item("Record JFR snapshot…", { recordJfr(vm) }) { testId = ID_ACTION_RECORD_JFR }
        }

    private fun recordJps(vm: VM) {
        val action = RecordJpsAction().apply { isCreateInboxItem = true }
        TriggerActionDialog.create(action, "Record JProfiler snapshot") {
            dispatch("Recording started. The snapshot will be delivered to your inbox.") { it.recordJps(vm, action) }
        }.open()
    }

    private fun recordJfr(vm: VM) {
        val action = RecordJfrAction().apply { isCreateInboxItem = true }
        TriggerActionDialog.create(action, "Record JFR snapshot") {
            dispatch("JFR recording started. The snapshot will be delivered to your inbox.") { it.recordJfr(vm, action) }
        }.open()
    }

    private fun runGc(vm: VM) =
        dispatch("Garbage collection triggered for ${vm.name}") { it.runGC(vm) }

    private fun confirmHeapDump(vm: VM) = confirm(
        "Create HPROF snapshot",
        "The HPROF snapshot will be delivered to your inbox. Creating it halts the JVM for a few seconds.",
        "Create"
    ) { dispatch("HPROF snapshot requested. Check your inbox.") { it.heapDump(vm) } }

    private fun confirmThreadDump(vm: VM) = confirm(
        "Create thread dump",
        "The thread dump will be delivered to your inbox.",
        "Create"
    ) { dispatch("Thread dump requested. Check your inbox.") { it.threadDump(vm) } }

    private fun dispatch(confirmation: String, action: (ServerConnection) -> Unit) {
        val connection = Sessions.current()?.serverConnection ?: return
        val ui = UI.getCurrent()
        Notifications.show(confirmation)
        runInBackground {
            try {
                action(connection)
            } catch (e: Exception) {
                // Surface permission denials as a toast; let real errors propagate (delivered via @Push).
                if (e is SecurityException || e is AccessDeniedException || e is AuthenticationException) {
                    ui.access { Notifications.show("Not allowed: ${e.message}") }
                } else {
                    throw e
                }
            }
        }
    }

    private fun canProfile(): Boolean {
        canProfile?.let { return it }
        return (Sessions.current()?.user?.accessLevel?.isAtLeast(AccessLevel.PROFILER) == true).also {
            canProfile = it
        }
    }

    companion object {
        const val ID_GRID = "vms-grid"
        const val ID_SHOW = "vm-show"
        const val ID_SHOW_TRANSACTIONS = "vm-show-transactions"
        const val ID_SHOW_TELEMETRIES = "vm-show-telemetries"
        const val ID_SHOW_MBEANS = "vm-show-mbeans"
        const val ID_ACTIONS = "vm-actions"
        const val ID_ACTION_GC = "vm-action-gc"
        const val ID_ACTION_HEAP_DUMP = "vm-action-heap-dump"
        const val ID_ACTION_THREAD_DUMP = "vm-action-thread-dump"
        const val ID_ACTION_RECORD_JPS = "vm-action-record-jps"
        const val ID_ACTION_RECORD_JFR = "vm-action-record-jfr"

        private const val SPARKLINE_COLUMN_WIDTH = "190px"

        private fun displayName(identifier: VmIdentifier): String {
            val name = identifier.name
            val slash = name.lastIndexOf('/')
            return if (slash >= 0) name.substring(slash + 1) else name
        }

        private fun formatSince(millis: Long): String {
            val seconds = maxOf(0L, millis) / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            return when {
                days > 0 -> "${days}d ${hours % 24}h"
                hours > 0 -> "${hours}h ${minutes % 60}m"
                minutes > 0 -> "${minutes}m"
                else -> "${seconds}s"
            }
        }

    }
}
