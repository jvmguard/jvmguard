package com.jvmguard.ui.components.recording.telemetries

import com.jvmguard.agent.config.telemetry.MBeanLineConfig
import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import com.jvmguard.common.helper.DeepCopy
import com.jvmguard.ui.components.*
import com.jvmguard.ui.components.recording.RecordingGrid
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.dnd.GridDropLocation
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.provider.hierarchy.TreeData
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider

class TelemetryGrid(
    private val telemetries: () -> MutableList<MBeanTelemetryConfig>,
    private val markChanged: () -> Unit,
) : RecordingGrid() {

    private val tree = TreeGrid<TelemetryNode>().apply {
        testId = ID_GRID
        addComponentHierarchyColumn(::nameCell).setHeader("Telemetry").setFlexGrow(1)
        addComponentColumn(::rowActions).setKey(ACTIONS_KEY).setAutoWidth(true).setFlexGrow(0)
        setEmptyStateComponent(emptyState("No telemetries yet. Use \"Add telemetry\" to create one."))
        addItemDoubleClickListener { editNode(it.item) }
        editDeleteKeys(::editNode, ::deleteNode)
        isAllRowsVisible = true
        onRowDrop(::onDrop)
    }

    init {
        isPadding = false
        isSpacing = false
        setWidthFull()
        add(tree)
        refresh()
    }

    override fun addNew() = addTelemetry()

    fun addTelemetry() {
        val config = MBeanTelemetryConfig()
        TelemetryConfigDialog(config, isNew = true, nameTaken = { nameExists(it, except = null) }) {
            telemetries().add(it)
            changed()
            confirm("Add a line?", "\"${it.name}\" has no lines yet. Add one now?", "Add line") { addLine(it) }
        }.open()
    }

    override fun refresh() {
        val data = TreeData<TelemetryNode>()
        val roots = telemetries().map { config ->
            val node = TelemetryNode.ConfigNode(config)
            data.addItem(null, node)
            config.lines.forEach { data.addItem(node, TelemetryNode.LineNode(it, config)) }
            node
        }
        tree.setDataProvider(TreeDataProvider(data))
        tree.expandRecursively(roots, 1)
    }

    private fun addLine(config: MBeanTelemetryConfig) {
        val line = MBeanLineConfig()
        TelemetryLineDialog(line, isNew = true, nameTaken = { lineNameExists(config, it, except = null) }) {
            config.lines.add(it)
            changed()
        }.open()
    }

    private fun nameCell(node: TelemetryNode): Component {
        val name = Span(nodeName(node))
        val detail = Span(detailText(node)).apply { addClassName("jvmguard-row-detail") }
        return cellRow(name, detail)
    }

    private fun nodeName(node: TelemetryNode): String = when (node) {
        is TelemetryNode.ConfigNode -> node.config.name.ifBlank { "(unnamed telemetry)" }
        is TelemetryNode.LineNode -> node.line.lineName.ifBlank { "(unnamed line)" }
    }

    private fun detailText(node: TelemetryNode): String = when (node) {
        is TelemetryNode.ConfigNode -> buildList {
            add(node.config.unit.toString())
            if (node.config.isStacked) {
                add("stacked")
            }
        }.joinToString(", ", prefix = "[", postfix = "]")

        is TelemetryNode.LineNode -> "[${node.line.beanName} / ${node.line.attributePath}]"
    }

    private fun rowActions(node: TelemetryNode): Component {
        val menu = menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions", "telemetry-row-menu-${nodeName(node)}") {
            addItem("Edit") { editNode(node) }
            addItem("Delete") { deleteNode(node) }
        }
        if (node !is TelemetryNode.ConfigNode) {
            return menu
        }
        val addLine = Button(VaadinIcon.PLUS.create()) { addLine(node.config) }.apply {
            addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL)
            setAriaLabel("Add line")
            setTooltipText("Add line")
            testId = "telemetry-add-line-${nodeName(node)}"
        }
        return cellRow(addLine, menu)
    }

    private fun editNode(node: TelemetryNode) = when (node) {
        is TelemetryNode.ConfigNode -> editConfig(node.config)
        is TelemetryNode.LineNode -> editLine(node.parent, node.line)
    }

    private fun editConfig(config: MBeanTelemetryConfig) {
        TelemetryConfigDialog(DeepCopy.clone(config), isNew = false, nameTaken = { nameExists(it, except = config) }) { saved ->
            val index = telemetries().indexOf(config)
            if (index >= 0) {
                telemetries()[index] = saved
            }
            changed()
        }.open()
    }

    private fun editLine(config: MBeanTelemetryConfig, line: MBeanLineConfig) {
        TelemetryLineDialog(DeepCopy.clone(line), isNew = false, nameTaken = { lineNameExists(config, it, except = line) }) { saved ->
            val index = config.lines.indexOf(line)
            if (index >= 0) {
                config.lines[index] = saved
            }
            changed()
        }.open()
    }

    private fun deleteNode(node: TelemetryNode) = when (node) {
        is TelemetryNode.ConfigNode -> confirmDelete("telemetry", nodeName(node)) {
            telemetries().remove(node.config)
            changed()
        }

        is TelemetryNode.LineNode -> confirmDelete("line", nodeName(node)) {
            node.parent.lines.remove(node.line)
            changed()
        }
    }

    private fun nameExists(name: String, except: MBeanTelemetryConfig?): Boolean =
        telemetries().any { it !== except && it.name == name.trim() }

    private fun lineNameExists(config: MBeanTelemetryConfig, name: String, except: MBeanLineConfig?): Boolean =
        config.lines.any { it !== except && it.lineName == name.trim() }

    private fun onDrop(source: TelemetryNode, target: TelemetryNode, location: GridDropLocation) {
        if (source is TelemetryNode.ConfigNode && target is TelemetryNode.ConfigNode) {
            reorder(telemetries(), source.config, target.config, location)
        } else if (source is TelemetryNode.LineNode && target is TelemetryNode.LineNode && source.parent === target.parent) {
            reorder(source.parent.lines, source.line, target.line, location)
        }
    }

    private fun <S> reorder(list: MutableList<S>, source: S, target: S, location: GridDropLocation) {
        moveWithin(list, source, target, location)
        changed()
    }

    private fun changed() {
        markChanged()
        refresh()
    }

    sealed class TelemetryNode {
        class ConfigNode(val config: MBeanTelemetryConfig) : TelemetryNode()
        class LineNode(val line: MBeanLineConfig, val parent: MBeanTelemetryConfig) : TelemetryNode()
    }

    companion object {
        const val ID_GRID = "telemetry-grid"
        const val ACTIONS_KEY = "telemetry-actions"
    }
}
