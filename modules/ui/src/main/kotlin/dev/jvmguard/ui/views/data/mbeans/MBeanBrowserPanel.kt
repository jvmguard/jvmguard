package dev.jvmguard.ui.views.data.mbeans

import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.ui.components.SelectableTreeGrid
import dev.jvmguard.ui.components.cellRow
import dev.jvmguard.ui.server.Sessions
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.splitlayout.SplitLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.provider.hierarchy.TreeData
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider
import com.vaadin.flow.data.value.ValueChangeMode
import javax.management.MBeanInfo

class MBeanBrowserPanel(
    private val onAttributeSelected: (AttributeNode?) -> Unit,
    private val onAttributeConfirmed: (AttributeNode) -> Unit,
) : SplitLayout() {

    private var vm: VM? = null
    private var mbeanNames: List<String> = emptyList()
    private val parents = mutableMapOf<AttributeNode, AttributeNode>()

    var selectedObjectName: String? = null
        private set

    private val filterField = TextField().apply {
        placeholder = "Filter MBeans"
        testId = ID_FILTER
        valueChangeMode = ValueChangeMode.LAZY
        isClearButtonVisible = true
        setWidthFull()
        prefixComponent = VaadinIcon.SEARCH.create()
        addValueChangeListener { rebuildTree() }
    }

    private val mbeanTree = SelectableTreeGrid<MBeanNode>().apply {
        testId = ID_TREE
        addComponentHierarchyColumn(::nameCell)
        setSizeFull()
        style.set("user-select", "none").set("-webkit-user-select", "none")
        addSelectionListener { event -> onNodeSelected(event.firstSelectedItem.orElse(null)) }
    }

    private val attributeTree = TreeGrid<AttributeNode>().apply {
        testId = ID_ATTRIBUTES
        addHierarchyColumn { it.name }.setHeader("Attribute").setFlexGrow(1).setSortable(false)
        addComponentColumn(::attributeValueCell).setHeader("Value").setFlexGrow(1).setSortable(false)
        setSizeFull()
        style.set("user-select", "none").set("-webkit-user-select", "none")
        addSelectionListener { onAttributeSelected(it.firstSelectedItem.orElse(null)) }
        addItemDoubleClickListener { onAttributeConfirmed(it.item) }
    }

    init {
        setSizeFull()
        setSplitterPosition(40.0)
        val filterWrapper = Div(filterField).apply {
            setWidthFull()
            style.set("padding-bottom", "0.5rem")
        }
        val left = VerticalLayout(filterWrapper, mbeanTree).apply {
            setSizeFull()
            isPadding = false
            isSpacing = false
            setFlexGrow(1.0, mbeanTree)
            style.set("min-width", "0").set("padding-inline-end", "var(--vaadin-padding-m)")
        }
        val right = VerticalLayout(attributeTree).apply {
            setSizeFull()
            isPadding = false
            isSpacing = false
            style.set("min-width", "0").set("padding-inline-start", "var(--vaadin-padding-m)")
        }
        addToPrimary(left)
        addToSecondary(right)
    }

    fun setVm(vm: VM) {
        this.vm = vm
        mbeanNames = Sessions.current()?.serverConnection?.getMBeanNames(vm, true)?.toList() ?: emptyList()
        rebuildTree()
    }

    fun parentOf(node: AttributeNode): AttributeNode? = parents[node]

    private fun rebuildTree() {
        populateMBeanTree(mbeanTree, mbeanNames, filterField.value.trim(), useRegex = false, matchCase = false)
        clearAttributes()
    }

    private fun onNodeSelected(node: MBeanNode?) {
        if (node is MBeanLeafNode) {
            loadAttributes(node.objectName)
        } else {
            clearAttributes()
        }
    }

    private fun loadAttributes(objectName: String) {
        val connection = Sessions.current()?.serverConnection ?: return
        val vm = vm ?: return
        val data = connection.getMBeanData(vm, objectName, fetchStructure = true, fetchValues = true)
        val beanInfo = data?.beanInfo
        if (beanInfo == null) {
            clearAttributes()
            return
        }
        selectedObjectName = objectName
        attributeTree.setDataProvider(TreeDataProvider(buildAttributeTree(beanInfo, data.values)))
        onAttributeSelected(null)
    }

    private fun buildAttributeTree(beanInfo: MBeanInfo, values: List<Any?>?): TreeData<AttributeNode> {
        parents.clear()
        val root = AttributeNode.buildTree(beanInfo, values ?: emptyList())
        val treeData = TreeData<AttributeNode>()
        addWithParents(treeData, null, root.children)
        return treeData
    }

    private fun addWithParents(treeData: TreeData<AttributeNode>, parent: AttributeNode?, nodes: List<AttributeNode>) {
        for (node in nodes) {
            treeData.addItem(parent, node)
            if (parent != null) {
                parents[node] = parent
            }
            addWithParents(treeData, node, node.children)
        }
    }

    private fun clearAttributes() {
        selectedObjectName = null
        parents.clear()
        attributeTree.setDataProvider(TreeDataProvider(TreeData()))
        onAttributeSelected(null)
    }

    private fun nameCell(node: MBeanNode): Component {
        val icon = (if (node is MBeanLeafNode) VaadinIcon.CUBE else VaadinIcon.FOLDER_O).create()
        icon.setSize("1em")
        return cellRow(icon, Span(node.name))
    }

    companion object {
        const val ID_FILTER = "mbean-picker-filter"
        const val ID_TREE = "mbean-picker-tree"
        const val ID_ATTRIBUTES = "mbean-picker-attributes"
    }
}
