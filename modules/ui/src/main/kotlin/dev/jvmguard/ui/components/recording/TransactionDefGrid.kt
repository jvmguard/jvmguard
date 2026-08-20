package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.*
import dev.jvmguard.common.helper.DeepCopy
import dev.jvmguard.ui.components.*
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.dnd.GridDropLocation
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.treegrid.TreeGrid
import com.vaadin.flow.data.provider.hierarchy.TreeData
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider

class TransactionDefGrid(
    private val type: TransactionType,
    private val showNaming: Boolean,
    private val defs: () -> MutableList<TransactionDef>,
    private val markChanged: () -> Unit,
) : VerticalLayout() {

    private val tree = TreeGrid<TxNode>().apply {
        testId = "transaction-grid-${type.name.lowercase()}"
        addHierarchyColumn(::nodeName).setHeader(t("recording.transaction.grid.header")).setFlexGrow(1)
        if (showNaming) {
            addComponentColumn(::namingCell).setHeader(t("recording.transaction.column.naming")).setAutoWidth(true).setFlexGrow(0)
        }
        addComponentColumn(::policiesCell).setHeader(t("recording.transaction.column.policies")).setAutoWidth(true).setFlexGrow(0)
        addComponentColumn(::rowActions).setAutoWidth(true).setFlexGrow(0)
        addItemDoubleClickListener { editNode(it.item) }
        editDeleteKeys(::editNode, ::deleteNode)
        setEmptyStateComponent(Span(t("recording.transaction.empty")).apply {
            addClassName("jvmguard-field-hint")
        })
        isAllRowsVisible = true
        onRowDrop(::onDrop)
    }

    init {
        isPadding = false
        isSpacing = false
        setWidthFull()
        add(Span(helpText(type)).apply { addClassName("jvmguard-transaction-type-info") })
        add(tree)
        refresh()
    }

    fun addDef() {
        val def = newDef()
        def.initDefault()
        openDefDialog(def, isNew = true) {
            defs().add(0, it)
            changed()
        }
    }

    fun refresh() {
        val data = TreeData<TxNode>()
        val roots = ownDefs().map { def ->
            val node = TxNode.DefNode(def)
            data.addItem(null, node)
            def.policySubDefs.forEach { data.addItem(node, TxNode.SubNode(it, def)) }
            node
        }
        tree.setDataProvider(TreeDataProvider(data))
        tree.expandRecursively(roots, 1)
    }

    private fun ownDefs(): List<TransactionDef> = defs().filter { it.transactionType == type }

    private fun nodeName(node: TxNode): String = when (node) {
        is TxNode.DefNode -> node.def.displayName
        is TxNode.SubNode -> {
            val filter = node.subDef.filter.ifBlank { t("recording.transaction.subNode.all") }
            if (node.subDef.isDiscard) t("recording.transaction.subNode.discard", filter) else filter
        }
    }

    private fun namingCell(node: TxNode): Component = when (node) {
        is TxNode.DefNode -> check(node.def.isNamingActive && !node.def.isDiscard)
        is TxNode.SubNode -> Span()
    }

    private fun policiesCell(node: TxNode): Component = when (node) {
        is TxNode.DefNode -> check(node.def.isPolicyActive && !node.def.isDiscard)
        is TxNode.SubNode -> check(node.subDef.policy.isActive && !node.subDef.isDiscard)
    }

    private fun check(on: Boolean): Component = if (on) VaadinIcon.CHECK.create().apply { setSize("1em") } else Span()

    private fun rowActions(node: TxNode): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, t("recording.actions"), "transaction-row-menu-${nodeName(node)}") {
            addItem(t("common.edit")) { editNode(node) }
            if (node is TxNode.DefNode) {
                addItem(t("recording.subdef.dialog.add")) { addSpec(node.def) }
            }
            addItem(t("common.delete")) { deleteNode(node) }
        }

    private fun editNode(node: TxNode) = when (node) {
        is TxNode.DefNode -> editDef(node.def)
        is TxNode.SubNode -> editSpec(node.parent, node.subDef)
    }

    private fun deleteNode(node: TxNode) = when (node) {
        is TxNode.DefNode -> confirm(t("recording.delete.transaction"), t("recording.delete.text", node.def.displayName), t("common.delete")) {
            defs().remove(node.def)
            changed()
        }

        is TxNode.SubNode -> confirm(t("recording.delete.spec"), t("recording.delete.spec.text"), t("common.delete")) {
            node.parent.policySubDefs.remove(node.subDef)
            changed()
        }
    }

    private fun editDef(def: TransactionDef) {
        openDefDialog(DeepCopy.clone(def), isNew = false) { saved ->
            val index = defs().indexOf(def)
            if (index >= 0) {
                defs()[index] = saved
            }
            changed()
        }
    }

    private fun openDefDialog(def: TransactionDef, isNew: Boolean, onSave: (TransactionDef) -> Unit) {
        when (type) {
            TransactionType.MATCHED -> MatchedTransactionDefDialog(def as MatchedTransactionDef, isNew) { onSave(it) }.open()
            TransactionType.DECLARED -> DeclaredTransactionDefDialog(def as DeclaredTransactionDef, isNew) { onSave(it) }.open()
            TransactionType.MAPPED -> MappedTransactionDefDialog(def as MappedTransactionDef, isNew) { onSave(it) }.open()
            else -> {}
        }
    }

    private fun newDef(): TransactionDef = when (type) {
        TransactionType.MATCHED -> MatchedTransactionDef()
        TransactionType.DECLARED -> DeclaredTransactionDef()
        TransactionType.MAPPED -> MappedTransactionDef()
        else -> throw IllegalStateException("Unsupported transaction type $type")
    }

    private fun addSpec(def: TransactionDef) {
        val sub = PolicySubDef(def)
        PolicySubDefDialog(sub, isNew = true) {
            def.policySubDefs.add(it)
            changed()
        }.open()
    }

    private fun editSpec(def: TransactionDef, sub: PolicySubDef) {
        PolicySubDefDialog(DeepCopy.clone(sub), isNew = false) { saved ->
            val index = def.policySubDefs.indexOf(sub)
            if (index >= 0) {
                def.policySubDefs[index] = saved
            }
            changed()
        }.open()
    }

    private fun onDrop(source: TxNode, target: TxNode, location: GridDropLocation) {
        if (source is TxNode.DefNode && target is TxNode.DefNode &&
            source.def.transactionType == type && target.def.transactionType == type
        ) {
            reorder(defs(), source.def, target.def, location)
        } else if (source is TxNode.SubNode && target is TxNode.SubNode && source.parent === target.parent) {
            reorder(source.parent.policySubDefs, source.subDef, target.subDef, location)
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

    private fun helpText(type: TransactionType): String = when (type) {
        TransactionType.MATCHED -> t("recording.transaction.help.matched")
        TransactionType.DECLARED -> t("recording.transaction.help.declared")
        TransactionType.MAPPED -> t("recording.transaction.help.mapped")
        else -> ""
    }

    sealed class TxNode {
        class DefNode(val def: TransactionDef) : TxNode()
        class SubNode(val subDef: PolicySubDef, val parent: TransactionDef) : TxNode()
    }
}
