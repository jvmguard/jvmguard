package dev.jvmguard.ui.views.data.mbeans

import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.provider.hierarchy.TreeData
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider
import javax.management.MBeanOperationInfo

class OperationResultDialog(
    operationInfo: MBeanOperationInfo,
    returnValue: Any?,
) : JvmGuardDialog() {

    init {
        headerTitle = t("mbeans.operation.result.title")
        width = "46rem"
        height = "32rem"

        val tree = TreeGrid<AttributeNode>().apply {
            addClassName("jvmguard-mbean-attribute-tree")
            addHierarchyColumn { it.name }.setHeader(t("vms.tree.name")).setFlexGrow(1).setSortable(false)
            addComponentColumn(::attributeValueCell).setHeader(t("mbeans.value.header")).setFlexGrow(1).setSortable(false)
            setSizeFull()
        }

        val root = AttributeNode.buildReturnValueTree(operationInfo.descriptor, returnValue)
        val treeData = TreeData<AttributeNode>()
        addAttributeNodes(treeData, null, root.children)
        tree.setDataProvider(TreeDataProvider(treeData))
        tree.expandRecursively(treeData.rootItems, Int.MAX_VALUE)

        add(tree)
        val ok = Button(t("common.ok")) { close() }.apply { addThemeVariants(ButtonVariant.PRIMARY) }
        ok.addClickShortcut(Key.ENTER).listenOn(this)
        footer.add(ok)
    }
}
