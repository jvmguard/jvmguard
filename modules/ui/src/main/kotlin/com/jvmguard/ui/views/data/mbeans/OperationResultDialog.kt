package com.jvmguard.ui.views.data.mbeans

import com.jvmguard.ui.components.JvmGuardDialog
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
        headerTitle = "Operation result"
        width = "46rem"
        height = "32rem"

        val tree = TreeGrid<AttributeNode>().apply {
            addClassName("jvmguard-mbean-attribute-tree")
            addHierarchyColumn { it.name }.setHeader("Name").setFlexGrow(1).setSortable(false)
            addComponentColumn(::attributeValueCell).setHeader("Value").setFlexGrow(1).setSortable(false)
            setSizeFull()
        }

        val root = AttributeNode.buildReturnValueTree(operationInfo.descriptor, returnValue)
        val treeData = TreeData<AttributeNode>()
        addAttributeNodes(treeData, null, root.children)
        tree.setDataProvider(TreeDataProvider(treeData))
        tree.expandRecursively(treeData.rootItems, Int.MAX_VALUE)

        add(tree)
        val ok = Button("OK") { close() }.apply { addThemeVariants(ButtonVariant.PRIMARY) }
        ok.addClickShortcut(Key.ENTER).listenOn(this)
        footer.add(ok)
    }
}
