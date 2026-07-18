package dev.jvmguard.ui.views.data.mbeans

import dev.jvmguard.agent.config.VmType
import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.components.*
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.findVm
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.ui.views.data.VmDataView
import dev.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.shared.Tooltip
import com.vaadin.flow.component.splitlayout.SplitLayout
import com.vaadin.flow.component.tabs.TabSheet
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.provider.hierarchy.TreeData
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
import java.rmi.RemoteException
import javax.management.MBeanAttributeInfo
import javax.management.MBeanInfo
import javax.management.MBeanOperationInfo
import javax.management.openmbean.SimpleType

@PermitAll
@Route(value = "mbeans", layout = MainLayout::class)
@PageTitle("jvmguard: MBeans")
class MBeansView : VmDataView() {

    private var mbeanNames: List<String> = emptyList()
    private var currentObjectName: String? = null
    private var currentVm: VM? = null

    private var attributeTreeData: TreeData<AttributeNode>? = null
    private var lastAttributeSignature: List<String>? = null
    private var refreshTicks = 0

    private val filterOptions = FilterOptionsMenu(ID_FILTER_OPTIONS) { rebuildIfFiltering() }

    private val filterField = TextField().apply {
        addClassName("jvmguard-mbean-filter")
        placeholder = "Filter MBeans"
        testId = ID_FILTER
        valueChangeMode = ValueChangeMode.LAZY
        isClearButtonVisible = true
        setWidthFull()
        prefixComponent = VaadinIcon.SEARCH.create()
        suffixComponent = filterOptions.button
        addValueChangeListener { rebuildTree() }
    }

    private val mbeanTree = SelectableTreeGrid<MBeanNode>().apply {
        testId = ID_TREE
        addClassName("jvmguard-mbean-tree")
        addComponentHierarchyColumn(::nameCell)
        setSizeFull()
        addSelectionListener { event ->
            val selected = event.firstSelectedItem.orElse(null)
            if (selected != null) onNodeSelected(selected) else clearAttributes()
        }
    }

    private val attributeTree = TreeGrid<AttributeNode>().apply {
        testId = ID_ATTRIBUTES
        addClassName("jvmguard-mbean-attribute-tree")
        addHierarchyColumn { it.name }.setHeader("Attribute").setFlexGrow(1).setSortable(false)
        addComponentColumn(::valueCell).setHeader("Value").setFlexGrow(1).setSortable(false)
        setSizeFull()
    }

    private val operationsGrid = Grid<MBeanOperationInfo>().apply {
        testId = ID_OPERATIONS
        addClassName("jvmguard-mbean-operations")
        addComponentColumn(::operationCell).setHeader("Operation").setFlexGrow(1).setSortable(false)
        addComponentColumn(::invokeCell).setHeader("").setFlexGrow(0).setAutoWidth(true)
        setSizeFull()
    }

    private val attributeInfoIcon = VaadinIcon.INFO_CIRCLE_O.create().apply {
        setSize(GRID_ICON_SIZE)
        addClassName("jvmguard-mbean-info-icon")
    }
    private val attributeTooltip: Tooltip = Tooltip.forComponent(attributeInfoIcon)
    private val attributeName = Span().apply { addClassName("jvmguard-mbean-attribute-name") }
    private val attributeClassName = Span().apply { addClassName("jvmguard-mbean-attribute-class") }

    private val detailTabs = TabSheet().apply {
        addClassName("jvmguard-mbean-tabs")
        setSizeFull()
        add("Attributes", attributeTree)
        add("Operations", operationsGrid)
    }

    private val attributeContent = VerticalLayout(
        HorizontalLayout(attributeInfoIcon, attributeName, attributeClassName).apply {
            addClassName("jvmguard-mbean-attribute-title")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isPadding = false
            isSpacing = false
            style.set("gap", "0.35em")
        },
        detailTabs,
    ).apply {
        setSizeFull()
        isPadding = false
        isSpacing = false
        setFlexGrow(1.0, detailTabs)
        style.set("min-width", "0")
    }

    private val attributeNotice = Span("Please select an MBean.").apply { addClassName("jvmguard-data-message") }

    private val detailPanel = VerticalLayout().apply {
        setSizeFull()
        isPadding = false
        isSpacing = false
        style.set("min-width", "0")
        style.set("box-sizing", "border-box")
        // gap towards the splitter
        style.set("padding-inline-start", "var(--vaadin-padding-m)")
    }

    private val message = Span().apply { addClassName("jvmguard-data-message") }

    private val split: SplitLayout

    init {
        val filterWrapper = Div(filterField).apply {
            setWidthFull()
            style.set("padding-bottom", "0.5rem")
        }
        val treePanel = VerticalLayout(filterWrapper, mbeanTree).apply {
            setSizeFull()
            isPadding = false
            isSpacing = false
            setFlexGrow(1.0, mbeanTree)
            style.set("min-width", "0")
            style.set("box-sizing", "border-box")
            style.set("padding-inline-end", "var(--vaadin-padding-m)")
        }
        split = SplitLayout(treePanel, detailPanel).apply {
            setSizeFull()
            style.set("min-height", "0")
            setSplitterPosition(34.0)
            addClassName("jvmguard-mbean-split")
        }
    }

    override fun onPollTick() {
        if (++refreshTicks >= REFRESH_EVERY_TICKS) {
            refreshTicks = 0
            refreshAttributes()
        }
    }

    override fun onSelectionChanged(selection: VmIdentifier) {
        if (selection.type == VmType.GROUP) {
            showSelectSingleVm()
        } else {
            loadMBeans()
        }
    }

    override fun isSelectable(selection: VmIdentifier): Boolean = selection.type != VmType.GROUP

    override fun selectorTitle(): String = "Select VM pool or VM"

    private fun loadMBeans() {
        val connection = Sessions.current()?.serverConnection ?: return showMessage(NOT_CONNECTED)
        val vm = resolveVm(connection)
            ?: return showMessage(if (currentSelection.type == VmType.POOL) NO_POOLED_VM else NO_VM)
        currentVm = vm
        try {
            mbeanNames = connection.getMBeanNames(vm, true).toList()
            rebuildTree()
            content.showFilling(split)
        } catch (e: RemoteException) {
            showMessage("Could not load MBeans: ${e.message}")
        }
    }

    private fun resolveVm(connection: ServerConnection): VM? {
        val vm = connection.findVm(currentSelection) ?: return null
        if (currentSelection.type == VmType.POOL) {
            return try {
                connection.getConnectedPooledVms(vm).firstOrNull()
            } catch (_: RemoteException) {
                null
            }
        }
        return vm
    }

    private fun rebuildTree() {
        populateMBeanTree(mbeanTree, mbeanNames, filterField.value.trim(), filterOptions.useRegex, filterOptions.matchCase)
        clearAttributes()
    }

    private fun onNodeSelected(node: MBeanNode) {
        if (node is MBeanLeafNode) {
            loadAttributes(node.objectName)
        } else {
            clearAttributes()
        }
    }

    private fun loadAttributes(objectName: String) {
        val connection = Sessions.current()?.serverConnection ?: return
        val vm = currentVm ?: return
        try {
            val keepExpansion = if (objectName == currentObjectName) expandedAttributePaths() else emptySet()
            val data = connection.getMBeanData(vm, objectName, fetchStructure = true, fetchValues = true)
            val beanInfo = data?.beanInfo
            if (beanInfo == null) {
                clearAttributes()
                return
            }
            currentObjectName = objectName
            attributeName.text = objectName
            attributeClassName.text = "[${beanInfo.className}]"
            val description = beanInfo.description
            attributeTooltip.text = description ?: ""
            attributeInfoIcon.isVisible = !description.isNullOrEmpty()

            val treeData = buildAttributeTree(beanInfo, data.values)
            showAttributeTree(treeData, keepExpansion)
            operationsGrid.setItems(beanInfo.operations.sortedBy { it.name })
            detailPanel.showFilling(attributeContent)
        } catch (e: RemoteException) {
            showMessage("Could not load MBean: ${e.message}")
        }
    }

    private fun refreshAttributes() {
        val objectName = currentObjectName ?: return
        if (detailTabs.selectedIndex != ATTRIBUTES_TAB) {
            return
        }
        val connection = Sessions.current()?.serverConnection ?: return
        val vm = currentVm ?: return
        try {
            val beanInfo = connection.getMBeanData(vm, objectName, fetchStructure = true, fetchValues = true)?.beanInfo ?: return
            val data = connection.getMBeanData(vm, objectName, fetchStructure = false, fetchValues = true)
            val treeData = buildAttributeTree(beanInfo, data?.values)
            if (attributeSignature(treeData) == lastAttributeSignature) {
                return
            }
            showAttributeTree(treeData, expandedAttributePaths())
        } catch (_: RemoteException) {
            // Transient — keep showing the last values until the next tick.
        }
    }

    private fun buildAttributeTree(beanInfo: MBeanInfo, values: List<Any?>?): TreeData<AttributeNode> {
        val root = AttributeNode.buildTree(beanInfo, values ?: emptyList())
        val treeData = TreeData<AttributeNode>()
        addAttributeNodes(treeData, null, root.children)
        return treeData
    }

    private fun showAttributeTree(treeData: TreeData<AttributeNode>, keepExpansion: Set<List<String>>) {
        attributeTree.setDataProvider(TreeDataProvider(treeData))
        attributeTreeData = treeData
        restoreAttributeExpansion(treeData, keepExpansion)
        lastAttributeSignature = attributeSignature(treeData)
    }

    /** A snapshot of the displayed values (per node, in tree order) used to detect real changes. */
    private fun attributeSignature(data: TreeData<AttributeNode>): List<String> {
        val signature = ArrayList<String>()
        fun visit(nodes: List<AttributeNode>) {
            for (node in nodes) {
                signature.add("${node.name}=${node.formattedValue()}")
                visit(data.getChildren(node))
            }
        }
        visit(data.rootItems)
        return signature
    }

    // Expansion is keyed by sibling index + display name so same-named siblings don't collide.
    private fun expandedAttributePaths(): Set<List<String>> {
        val data = attributeTreeData ?: return emptySet()
        val paths = mutableSetOf<List<String>>()
        fun visit(nodes: List<AttributeNode>, prefix: List<String>) {
            nodes.forEachIndexed { index, node ->
                val path = prefix + "$index\u0000${node.name}"
                if (attributeTree.isExpanded(node)) {
                    paths.add(path)
                }
                visit(data.getChildren(node), path)
            }
        }
        visit(data.rootItems, emptyList())
        return paths
    }

    private fun restoreAttributeExpansion(data: TreeData<AttributeNode>, paths: Set<List<String>>) {
        if (paths.isEmpty()) {
            return
        }
        fun visit(nodes: List<AttributeNode>, prefix: List<String>) {
            nodes.forEachIndexed { index, node ->
                val path = prefix + "$index\u0000${node.name}"
                if (path in paths) {
                    attributeTree.expand(node)
                }
                visit(data.getChildren(node), path)
            }
        }
        visit(data.rootItems, emptyList())
    }

    private fun clearAttributes() {
        attributeTree.setDataProvider(TreeDataProvider(TreeData()))
        attributeTreeData = null
        lastAttributeSignature = null
        currentObjectName = null
        detailPanel.showCentered(attributeNotice)
    }

    private fun nameCell(node: MBeanNode): Component {
        val icon = (if (node is MBeanLeafNode) VaadinIcon.CUBE else VaadinIcon.FOLDER_O).create()
        icon.setSize(GRID_ICON_SIZE)
        return cellRow(icon, Span(node.name))
    }

    private fun valueCell(node: AttributeNode): Component {
        val valueSpan = attributeValueSpan(node)
        val attributeInfo = node.attributeInfo
        if (attributeInfo != null) {
            valueSpan.testId = "$ID_VALUE_PREFIX${attributeInfo.name}"
        }
        val detail = valueDetailButton(node)
        val edit = if (node.writable && attributeInfo != null) editButton(node, attributeInfo) else null
        if (detail == null && edit == null) {
            return valueSpan
        }
        return valueRow(valueSpan).apply {
            detail?.let { add(it); toggleOnOverflow(valueSpan, it) }
            edit?.let { add(it) }
        }
    }

    private fun editButton(node: AttributeNode, attributeInfo: MBeanAttributeInfo): Button =
        Button(VaadinIcon.EDIT.create()) { openAttributeEditor(node, attributeInfo) }.apply {
            addThemeVariants(ButtonVariant.TERTIARY)
            addClassName("jvmguard-field-icon-button")
            setAriaLabel("Edit value")
            setTooltipText("Edit value")
            testId = "$ID_EDIT_PREFIX${attributeInfo.name}"
        }

    private fun openAttributeEditor(node: AttributeNode, attributeInfo: MBeanAttributeInfo) {
        val spec = ValueEditSpec(node.openType, attributeInfo.type, node.name, node.value)
        AttributeEditDialog(spec) { value -> commitAttribute(attributeInfo, value) }.open()
    }

    private fun commitAttribute(attributeInfo: MBeanAttributeInfo, value: Any?) {
        val connection = Sessions.current()?.serverConnection ?: return
        val vm = currentVm ?: return
        val objectName = currentObjectName ?: return
        try {
            val result = connection.setMBeanAttribute(vm, objectName, attributeInfo, value)
            val error = result.errorMessage
            if (error != null) {
                showError("Could not set attribute", error, result.stackTrace)
            }
            loadAttributes(objectName)
        } catch (e: RemoteException) {
            showError("Could not set attribute", e.message ?: e.toString(), null)
        }
    }

    private fun operationCell(operationInfo: MBeanOperationInfo): Component {
        val name = Span(operationInfo.name)
        val signature = Span(MBeanOperations.signatureSuffix(operationInfo)).apply {
            addClassName("jvmguard-mbean-operation-signature")
        }
        return HorizontalLayout(name, signature).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isSpacing = false
            isPadding = false
            style.set("gap", "0.25em")
            operationInfo.description?.takeIf { it.isNotEmpty() }?.let { Tooltip.forComponent(this).text = it }
        }
    }

    private fun invokeCell(operationInfo: MBeanOperationInfo): Component =
        Button(VaadinIcon.PLAY.create()) { invokeOperation(operationInfo) }.apply {
            addThemeVariants(ButtonVariant.TERTIARY)
            addClassName("jvmguard-field-icon-button")
            setAriaLabel("Invoke operation")
            setTooltipText("Invoke operation")
            testId = "$ID_INVOKE_PREFIX${operationInfo.name}"
        }

    private fun invokeOperation(operationInfo: MBeanOperationInfo) {
        val specs = try {
            MBeanOperations.valueEditSpecs(operationInfo)
        } catch (_: NonOpenTypeException) {
            Notification.show("Cannot invoke: the operation's parameters are not all open types.")
            return
        }
        if (specs.isEmpty()) {
            runOperation(operationInfo, emptyList())
        } else {
            ParameterDialog(operationInfo, specs) { values -> runOperation(operationInfo, values) }.open()
        }
    }

    private fun runOperation(operationInfo: MBeanOperationInfo, values: List<Any?>) {
        val connection = Sessions.current()?.serverConnection ?: return
        val vm = currentVm ?: return
        val objectName = currentObjectName ?: return
        try {
            val data = connection.invokeMBeanOperation(vm, objectName, operationInfo, values.toTypedArray())
            val error = data.errorMessage
            when {
                error != null -> showError("Operation failed", error, data.stackTrace)
                isVoid(operationInfo) -> Notification.show("The operation was invoked successfully.")
                else -> OperationResultDialog(operationInfo, data.returnValue).open()
            }
        } catch (e: RemoteException) {
            showError("Operation failed", e.message ?: e.toString(), null)
        }
    }

    private fun isVoid(operationInfo: MBeanOperationInfo): Boolean =
        operationInfo.returnType == "void" || MBeanOperations.returnOpenType(operationInfo) == SimpleType.VOID

    private fun rebuildIfFiltering() {
        if (filterField.value.isNotEmpty()) {
            rebuildTree()
        }
    }

    private fun showSelectSingleVm() {
        currentObjectName = null
        currentVm = null
        val selectButton = Button("Select a VM", VaadinIcon.SEARCH.create()) { openSelector() }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_SELECT_VM
        }
        val prompt = VerticalLayout(
            Span("Please select a single named VM or a VM pool first."),
            selectButton,
        ).apply {
            isPadding = false
            alignItems = FlexComponent.Alignment.CENTER
            style.set("gap", "0.75rem")
        }
        content.showCentered(prompt)
    }

    private fun showMessage(text: String) {
        message.text = text
        content.showCentered(message)
    }

    private fun showError(title: String, errorMessage: String, stackTrace: String?) =
        ErrorDialog(title, errorMessage, stackTrace).open()

    companion object {
        const val ID_TREE = "mbean-tree"
        const val ID_ATTRIBUTES = "mbean-attributes"
        const val ID_OPERATIONS = "mbean-operations"

        const val ID_VALUE_PREFIX = "mbean-value-"
        const val ID_EDIT_PREFIX = "mbean-edit-"
        const val ID_INVOKE_PREFIX = "mbean-invoke-"
        const val ID_FILTER = "mbean-filter"
        const val ID_FILTER_OPTIONS = "mbean-filter-options"
        const val ID_SELECT_VM = "mbean-select-vm"

        private const val ATTRIBUTES_TAB = 0

        // every 2s.
        private const val REFRESH_EVERY_TICKS = 4

        private const val NOT_CONNECTED = "Not connected to the jvmguard server."
        private const val NO_VM = "The selected JVM is not available."
        private const val NO_POOLED_VM = "No connected JVM in this pool."
    }
}
