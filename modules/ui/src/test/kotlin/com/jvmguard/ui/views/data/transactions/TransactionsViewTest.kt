package com.jvmguard.ui.views.data.transactions

import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import com.jvmguard.data.transactions.TimeRequirement
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.components.echart.EChart
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.router.QueryParameters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionsViewTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setCurrent(UserSession(MockConnections.create()))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    @Test
    fun rendersTheCallTreeForTheRoot() {
        UI.getCurrent().navigate(TransactionsView::class.java)

        val roots = rootNodes()
        assertTrue(roots.any { it.name == "Request 1" }, roots.map { it.name }.toString())
        val request = roots.first { it.name == "Request 1" }
        assertTrue(request.time > 0, "top-level transaction should carry a total time")
        assertTrue(request.children.any { it.name == "Child 1" }, "call tree should expand into child methods")
    }

    @Test
    fun opensInTheModeFromDrillDownTarget() {
        TransactionDrill.set(1000L, TransactionMode.HOT_SPOTS)
        UI.getCurrent().navigate(TransactionsView::class.java)

        val tabs = find<Tabs>().single()
        assertEquals(TransactionMode.HOT_SPOTS.ordinal, tabs.selectedIndex)
    }

    @Test
    fun marksNonNormalPolicyWithAPrefix() {
        UI.getCurrent().navigate(TransactionsView::class.java)
        val child = rootNodes().first { it.name == "Request 1" }.children.first { it.name == "Child 1" }
        assertEquals("[Very slow]", child.policyPrefix)
    }

    @Test
    fun rendersTheModeTabsAndDefaultColumnHeader() {
        UI.getCurrent().navigate(TransactionsView::class.java)
        val tabs = find<Tabs>().single()
        val labels = (0 until tabs.tabCount).map { (tabs.getTabAt(it) as Tab).label }
        assertEquals(listOf("Call tree", "Hot spots", "Overdue"), labels)
        assertEquals("Transaction", treeGrid().columns.first().headerText)
    }

    @Test
    fun filterReducesTheVisibleTransactions() {
        UI.getCurrent().navigate(TransactionsView::class.java)
        use(find<TextField>().single()).setValue("Request 2")

        val names = rootNodes().map { it.name }
        assertEquals(listOf("Request 2"), names, names.toString())
    }

    @Test
    fun statusFilterPrunesTheTreeByPolicy() {
        val connection = Sessions.current()!!.serverConnection
        val cursor = connection.getCurrentTransactionTreeCursor(
            connection.namedVms.first(), TransactionTreeInterval.TEN_MINUTE, TransactionDataType.TRANSACTION
        )
        val roots = TransactionNode.roots(connection.getCallTree(cursor, true), cumulateBacktraces = false)

        // Mock: every request has a VERY_SLOW "Child 1" and nothing is ERROR.
        val verySlow = roots.mapNotNull { it.filtered { node -> node.policyType == PolicyType.VERY_SLOW } }
        assertTrue(verySlow.isNotEmpty(), "very-slow filter should keep the requests with a very-slow child")
        assertTrue(verySlow.all { root -> root.children.all { it.policyType == PolicyType.VERY_SLOW } })
        assertTrue(roots.mapNotNull { it.filtered { node -> node.policyType == PolicyType.ERROR } }.isEmpty())
    }

    @Test
    fun hotSpotsGroupBacktracesUnderACumulatedNode() {
        val connection = Sessions.current()!!.serverConnection
        val cursor = connection.getCurrentTransactionTreeCursor(
            connection.namedVms.first(), TransactionTreeInterval.TEN_MINUTE, TransactionDataType.TRANSACTION
        )

        val hotspots = TransactionNode.roots(connection.getHotspots(cursor, true), cumulateBacktraces = true)
        val withChildren = hotspots.filter { it.children.isNotEmpty() }
        assertTrue(withChildren.isNotEmpty(), "the mock hot spots have backtraces")
        withChildren.forEach { node ->
            val only = node.children.single()
            assertTrue(only.isContainer, "backtraces are grouped under a synthetic container")
            assertEquals(TransactionNode.BACKTRACE_CONTAINER, only.name)
            assertTrue(only.children.isNotEmpty(), "the container holds the actual backtraces")
        }

        val callTree = TransactionNode.roots(connection.getCallTree(cursor, true), cumulateBacktraces = false)
        assertTrue(callTree.none { root -> root.children.any { it.isContainer } })
    }

    @Test
    fun rendersForASingleVm() {
        UI.getCurrent().navigate(
            TransactionsView::class.java, QueryParameters.simple(mapOf("vm" to "Database/DB 01"))
        )
        assertTrue(rootNodes().isNotEmpty(), "single-JVM transactions should render rows")
    }

    @Test
    fun showsTheInPlaceTimelineChart() {
        UI.getCurrent().navigate(TransactionsView::class.java)
        assertNotNull(find<EChart>().single())
    }

    @Test
    fun startTimeCursorResolvesToRecordedData() {
        // START_TIME cursors must resolve to canned data or the per-transaction time line is empty.
        val connection = Sessions.current()!!.serverConnection
        val interval = TransactionTreeInterval.TEN_MINUTE
        val recentTime = connection.currentTime - interval.timeExtent
        val cursor = connection.getTransactionTreeCursor(
            connection.namedVms.first(), interval, TransactionDataType.TRANSACTION, recentTime, TimeRequirement.START_TIME
        )
        assertTrue(
            cursor.availability.isAvailable,
            "a START_TIME cursor within the recorded window must be available"
        )
    }

    private fun rootNodes(): List<TransactionNode> = treeGrid().treeData.rootItems

    @Suppress("UNCHECKED_CAST")
    private fun treeGrid(): TreeGrid<TransactionNode> =
        find<TreeGrid<*>>().single() as TreeGrid<TransactionNode>
}
