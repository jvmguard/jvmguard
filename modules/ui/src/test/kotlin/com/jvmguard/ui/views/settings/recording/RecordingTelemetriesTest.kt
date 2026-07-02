package com.jvmguard.ui.views.settings.recording

import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import com.jvmguard.common.helper.ListModification
import com.jvmguard.data.config.sets.TelemetrySet
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.components.SelectableTreeGrid
import com.jvmguard.ui.components.recording.sets.AddSetDialog
import com.jvmguard.ui.components.recording.sets.SaveSetDialog
import com.jvmguard.ui.components.recording.telemetries.NumericMBeanValueDialog
import com.jvmguard.ui.components.recording.telemetries.TelemetryConfigDialog
import com.jvmguard.ui.components.recording.telemetries.TelemetryGrid
import com.jvmguard.ui.components.recording.telemetries.TelemetryLineDialog
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.ui.views.data.VmSelectorDialog
import com.jvmguard.ui.views.data.mbeans.AttributeNode
import com.jvmguard.ui.views.data.mbeans.MBeanLeafNode
import com.jvmguard.ui.views.data.mbeans.MBeanNode
import com.jvmguard.ui.views.data.mbeans.OpenTypeHelper
import com.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.confirmdialog.ConfirmDialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.provider.hierarchy.TreeData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordingTelemetriesTest : JvmGuardBrowserlessTest() {

    private lateinit var connection: ServerConnection

    @BeforeEach
    fun setUp() {
        connection = MockConnections.create(AccessLevel.ADMIN)
        Sessions.setCurrent(UserSession(connection))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.clearRecordingDraft()
        Sessions.resetRecordingSelection()
    }

    private fun rootTelemetries() =
        Sessions.recordingDraft().groupConfig(VmIdentifier.ROOT_GROUP_IDENTIFIER)!!.telemetrySettings.mbeanTelemetries

    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }

    private fun addTelemetry(name: String) {
        find<TelemetryGrid>().single().addTelemetry()
        val dialog = find<TelemetryConfigDialog>().single()
        use(find<TextField>(dialog).all().first { it.label == "Name" }).setValue(name)
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()
        // A new telemetry prompts to add a line; tests that need one add it explicitly.
        find<ConfirmDialog>().all().firstOrNull()?.close()
    }

    private fun testId(id: String): (com.vaadin.flow.component.Component) -> Boolean = { it.testId == id }

    @Test
    fun rootShowsTelemetryGrid() {
        UI.getCurrent().navigate(RecordingTelemetriesView::class.java)
        find<TelemetryGrid>().single()
    }

    @Test
    fun addingATelemetryPersistsToTheGroup() {
        UI.getCurrent().navigate(RecordingTelemetriesView::class.java)
        addTelemetry("GC durations")

        assertTrue(rootTelemetries().any { it.name == "GC durations" })

        use(shellSave()).click()
        val saved = connection.groupConfigs.first { it.isRoot }.telemetrySettings.mbeanTelemetries
        assertTrue(saved.any { it.name == "GC durations" })
    }

    @Test
    fun savingASetStoresItOnTheServer() {
        UI.getCurrent().navigate(RecordingTelemetriesView::class.java)
        addTelemetry("Heap")

        use(find<Button>().all().first { it.text == "Save set" }).click()
        val dialog = find<SaveSetDialog<*, *>>().single()
        use(find<TextField>(dialog).all().first()).setValue("Memory metrics")
        use(find<Button>(dialog).all().first { it.text == "Save" }).click()

        assertTrue(connection.telemetrySets.any { it.name == "Memory metrics" && it.items.any { c -> c.name == "Heap" } })
    }

    @Test
    fun addingASetAppendsItsTelemetries() {
        val set = TelemetrySet("Imported", listOf(MBeanTelemetryConfig().apply { name = "Imported telemetry" }))
        connection.applyListModification(ListModification(emptyList(), emptyList(), listOf(set), TelemetrySet::class.java))

        UI.getCurrent().navigate(RecordingTelemetriesView::class.java)
        use(find<Button>().all().first { it.text == "Add set" }).click()

        val dialog = find<AddSetDialog<*, *>>().single()

        @Suppress("UNCHECKED_CAST")
        val grid = find<Grid<*>>(dialog).single() as Grid<TelemetrySet>
        grid.select(grid.genericDataView.items.toList().first { it.name == "Imported" })
        use(find<Button>(dialog).all().first { it.text == "Add" }).click()

        assertTrue(rootTelemetries().any { it.name == "Imported telemetry" })
    }

    @Test
    fun pickingANumericMBeanValueFillsAndSavesALine() {
        UI.getCurrent().navigate(RecordingTelemetriesView::class.java)
        addTelemetry("Memory")

        // Grid-cell components aren't reachable via find; go through getCellComponent.
        @Suppress("UNCHECKED_CAST")
        val telemetryTree = find<TreeGrid<*>>().all().first() as TreeGrid<Any>
        val actionsCell = use(telemetryTree).getCellComponent(0, TelemetryGrid.ACTIONS_KEY)
        use(find<Button>(actionsCell).all().first(testId("telemetry-add-line-Memory"))).click()

        val lineDialog = find<TelemetryLineDialog>().single()
        use(find<Button>(lineDialog).all().first { it.text == "Select" }).click()

        // The picker needs a VM first.
        val picker = find<NumericMBeanValueDialog>().single()
        use(find<Button>(picker).all().first(testId(NumericMBeanValueDialog.ID_SELECT_VM))).click()
        val vmSelector = find<VmSelectorDialog>().single()

        @Suppress("UNCHECKED_CAST")
        val vmTree = find<SelectableTreeGrid<*>>(vmSelector).single() as SelectableTreeGrid<VmIdentifier>
        vmTree.select(vmIdentifiers(vmTree.treeData).first { it.type != VmType.GROUP })
        use(find<Button>(vmSelector).all().first { it.text == "Select" }).click()

        @Suppress("UNCHECKED_CAST")
        val mbeanTree = find<SelectableTreeGrid<*>>(picker).single() as SelectableTreeGrid<MBeanNode>

        @Suppress("UNCHECKED_CAST")
        val attributeTree = find<TreeGrid<*>>(picker).all().first { it !is SelectableTreeGrid<*> } as TreeGrid<AttributeNode>

        val mbeanLeaves = leaves(mbeanTree.treeData)
        assertTrue(mbeanLeaves.isNotEmpty(), "the picker should list the platform MBeans")
        var numeric: AttributeNode? = null
        var bean: MBeanLeafNode? = null
        for (leaf in mbeanLeaves) {
            mbeanTree.select(leaf)
            numeric = attributeTree.treeData.rootItems.firstOrNull { OpenTypeHelper.isNumberType(it.openType) }
            if (numeric != null) {
                bean = leaf
                break
            }
        }
        assertTrue(numeric != null, "at least one MBean should expose a numeric attribute")
        attributeTree.select(numeric)

        val confirm = find<Button>(picker).all().first { it.text == "Select" }
        assertTrue(confirm.isEnabled, "selecting a numeric attribute should enable the picker")
        use(confirm).click()

        use(find<Button>(lineDialog).all().first { it.text == "Save" }).click()

        val telemetry = rootTelemetries().first { it.name == "Memory" }
        assertTrue(telemetry.lines.any { it.beanName == bean!!.objectName && it.attributePath.isNotEmpty() })
    }

    @Test
    fun numericPickerRejectsNonNumericValues() {
        val dialog = NumericMBeanValueDialog { _, _, _ -> }
        dialog.open()
        assertFalse(find<Button>(dialog).all().first { it.text == "Select" }.isEnabled)
    }

    private fun leaves(data: TreeData<MBeanNode>): List<MBeanLeafNode> {
        val result = mutableListOf<MBeanLeafNode>()
        fun visit(nodes: List<MBeanNode>) {
            for (node in nodes) {
                if (node is MBeanLeafNode) {
                    result.add(node)
                }
                visit(data.getChildren(node))
            }
        }
        visit(data.rootItems)
        return result
    }

    private fun vmIdentifiers(data: TreeData<VmIdentifier>): List<VmIdentifier> {
        val result = mutableListOf<VmIdentifier>()
        fun visit(nodes: List<VmIdentifier>) {
            for (node in nodes) {
                result.add(node)
                visit(data.getChildren(node))
            }
        }
        visit(data.rootItems)
        return result
    }
}
