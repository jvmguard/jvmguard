package com.jvmguard.ui.views.data.mbeans

import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.components.SelectableTreeGrid
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.ui.views.data.VmSelectorDialog
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.tabs.TabSheet
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.provider.hierarchy.TreeData
import com.vaadin.flow.router.QueryParameters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MBeansViewTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setCurrent(UserSession(MockConnections.create()))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    @Test
    fun rendersTheMBeanTreeForASingleVm() {
        // The mock serves this JVM's own platform MBeans.
        UI.getCurrent().navigate(
            MBeansView::class.java, QueryParameters.simple(mapOf("vm" to "Database/DB 01"))
        )

        @Suppress("UNCHECKED_CAST")
        val tree = find<SelectableTreeGrid<*>>().single() as SelectableTreeGrid<MBeanNode>
        assertTrue(tree.treeData.rootItems.isNotEmpty(), "the platform MBeans should populate the tree")
    }

    @Test
    fun selectingAnMBeanLoadsItsAttributes() {
        UI.getCurrent().navigate(
            MBeansView::class.java, QueryParameters.simple(mapOf("vm" to "Database/DB 01"))
        )

        @Suppress("UNCHECKED_CAST")
        val tree = find<SelectableTreeGrid<*>>().single() as SelectableTreeGrid<MBeanNode>
        val leaves = collectLeaves(tree.treeData)
        val leaf = leaves.firstOrNull { it.objectName.contains("type=Runtime") } ?: leaves.first()
        tree.select(leaf)

        // The attribute tree is the second, non-selectable TreeGrid.
        val attributeTree = find<TreeGrid<*>>().all().first { it !is SelectableTreeGrid<*> }
        assertTrue(attributeTree.treeData.rootItems.isNotEmpty(), "selecting an MBean should load its attributes")
    }

    @Test
    fun selectingAnMBeanLoadsItsOperations() {
        selectBean(MEMORY)

        // The operations grid is attached only once its TabSheet tab is selected.
        use(find<TabSheet>().single()).select(1)
        val operationsGrid = find<Grid<*>>().all().first { it !is TreeGrid<*> }
        assertTrue(use(operationsGrid).size() > 0, "the Memory MBean exposes operations (gc)")
    }

    @Test
    fun writableAttributeIsMarkedEditable() {
        selectBean(MEMORY)

        @Suppress("UNCHECKED_CAST")
        val attributeTree = find<TreeGrid<*>>().all().first { it !is SelectableTreeGrid<*> } as TreeGrid<AttributeNode>
        val verbose = attributeTree.treeData.rootItems.first { it.name == "Verbose" }
        assertTrue(verbose.writable, "Memory.Verbose is a writable boolean")
    }

    private fun selectBean(objectName: String) {
        UI.getCurrent().navigate(
            MBeansView::class.java, QueryParameters.simple(mapOf("vm" to "Database/DB 01"))
        )
        @Suppress("UNCHECKED_CAST")
        val tree = find<SelectableTreeGrid<*>>().single() as SelectableTreeGrid<MBeanNode>
        tree.select(collectLeaves(tree.treeData).first { it.objectName == objectName })
    }

    private fun collectLeaves(treeData: TreeData<MBeanNode>): List<MBeanLeafNode> {
        val leaves = mutableListOf<MBeanLeafNode>()
        fun visit(nodes: List<MBeanNode>) {
            for (node in nodes) {
                if (node is MBeanLeafNode) leaves.add(node)
                visit(treeData.getChildren(node))
            }
        }
        visit(treeData.rootItems)
        return leaves
    }

    @Test
    fun groupSelectionDoesNotShowTheTree() {
        // The MBean browser is single-VM; at a group it prompts to pick a JVM instead.
        UI.getCurrent().navigate(MBeansView::class.java)

        assertTrue(
            find<SelectableTreeGrid<*>>().all().isEmpty(),
            "a group selection must not show the MBean tree"
        )
    }

    @Test
    fun theSelectorDialogIsScopedToPoolsAndVms() {
        UI.getCurrent().navigate(MBeansView::class.java)
        use(find<Button>().all().first { it.text == "Select a VM" }).click()

        assertEquals("Select VM pool or VM", find<VmSelectorDialog>().single().headerTitle)
    }

    private companion object {
        const val MEMORY = "java.lang:type=Memory"
    }
}
