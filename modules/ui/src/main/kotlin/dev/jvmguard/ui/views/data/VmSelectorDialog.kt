package dev.jvmguard.ui.views.data

import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.server.Sessions
import com.vaadin.flow.data.provider.hierarchy.TreeData

class VmSelectorDialog(
    current: VmIdentifier,
    onSelect: (VmIdentifier) -> Unit,
    selectable: (VmIdentifier) -> Boolean = { true },
    title: String = "Select group or JVM",
) : AbstractVmSelectorDialog(current, onSelect, selectable, title) {

    override fun configureColumns() {
        // suppress the empty header row.
        tree.addClassName("jvmguard-selector-tree")
        addNameColumn()
    }

    override fun buildTreeData(): TreeData<VmIdentifier> {
        val identifiers = Sessions.current()?.serverConnection?.namedVms?.map { it.qualifiedIdentifier } ?: emptyList()
        return treeDataOf(identifiers)
    }

    companion object {
        const val ID_SELECT = AbstractVmSelectorDialog.ID_SELECT
    }
}
