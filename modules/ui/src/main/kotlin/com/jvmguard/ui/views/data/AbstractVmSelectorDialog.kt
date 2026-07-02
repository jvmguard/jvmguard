package com.jvmguard.ui.views.data

import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.components.JvmGuardDialog
import com.jvmguard.ui.components.SelectableTreeGrid
import com.jvmguard.ui.components.vmTypeIcon
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.data.provider.hierarchy.TreeData
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider

abstract class AbstractVmSelectorDialog(
    current: VmIdentifier,
    private val onSelect: (VmIdentifier) -> Unit,
    private val selectable: (VmIdentifier) -> Boolean,
    title: String,
    dialogWidth: String = "34rem",
    dialogHeight: String = "26rem",
    expandAll: Boolean = false,
) : JvmGuardDialog() {

    protected val tree = SelectableTreeGrid<VmIdentifier>()

    init {
        headerTitle = title
        width = dialogWidth
        height = dialogHeight

        tree.style.set("user-select", "none").set("-webkit-user-select", "none")
        configureColumns()
        tree.setSizeFull()
        val treeData = buildTreeData()
        tree.setDataProvider(TreeDataProvider(treeData))
        tree.addItemDoubleClickListener { if (selectable(it.item)) confirm(it.item) }

        val select = Button("Select") {
            tree.asSingleSelect().value?.takeIf(selectable)?.let(::confirm)
        }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_SELECT
        }
        tree.addSelectionListener { event -> select.isEnabled = event.firstSelectedItem.map(selectable).orElse(false) ?: false }
        select.addClickShortcut(Key.ENTER).listenOn(this)

        if (expandAll) {
            tree.expandRecursively(treeData.rootItems, Int.MAX_VALUE)
        } else {
            tree.expand(VmIdentifier.ROOT_GROUP_IDENTIFIER)
            var ancestor = current.parent
            while (ancestor != null) {
                tree.expand(ancestor)
                ancestor = ancestor.parent
            }
        }
        tree.select(current)

        add(tree)
        footer.add(Button("Cancel") { close() }, select)
    }

    protected abstract fun configureColumns()

    protected abstract fun buildTreeData(): TreeData<VmIdentifier>

    protected fun treeDataOf(identifiers: Iterable<VmIdentifier>): TreeData<VmIdentifier> {
        val treeData = TreeData<VmIdentifier>()
        val added = mutableSetOf<VmIdentifier>()
        fun ensure(identifier: VmIdentifier) {
            if (!added.add(identifier)) {
                return
            }
            val parent = identifier.parent
            if (parent == null) {
                treeData.addItem(null, identifier)
            } else {
                ensure(parent)
                treeData.addItem(parent, identifier)
            }
        }
        ensure(VmIdentifier.ROOT_GROUP_IDENTIFIER)
        identifiers.forEach(::ensure)
        return treeData
    }

    protected fun addNameColumn(): Grid.Column<VmIdentifier> = tree.addComponentHierarchyColumn(::nodeRow)

    protected open fun nodeRow(identifier: VmIdentifier): Component {
        val icon = vmTypeIcon(identifier.type).create().apply { setSize("1.2em") }
        val name = if (identifier.isRoot) "All JVMs" else identifier.toUnqualified().name
        return HorizontalLayout(icon, Span(name)).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isSpacing = false
            isPadding = false
            style.set("gap", "0.4em")
        }
    }

    private fun confirm(identifier: VmIdentifier) {
        onSelect(identifier)
        close()
    }

    companion object {
        const val ID_SELECT = "vm-selector-select"
    }
}
