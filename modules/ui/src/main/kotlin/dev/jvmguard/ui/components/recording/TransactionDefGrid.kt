package dev.jvmguard.ui.components.recording

import dev.jvmguard.agent.config.transactions.*
import dev.jvmguard.common.helper.DeepCopy
import dev.jvmguard.ui.components.*
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
        addHierarchyColumn(::nodeName).setHeader("Business transaction").setFlexGrow(1)
        if (showNaming) {
            addComponentColumn(::namingCell).setHeader("Naming").setAutoWidth(true).setFlexGrow(0)
        }
        addComponentColumn(::policiesCell).setHeader("Policies").setAutoWidth(true).setFlexGrow(0)
        addComponentColumn(::rowActions).setAutoWidth(true).setFlexGrow(0)
        addItemDoubleClickListener { editNode(it.item) }
        editDeleteKeys(::editNode, ::deleteNode)
        setEmptyStateComponent(Span("No transactions yet. Use \"Add transaction\" to create one.").apply {
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
            val prefix = if (node.subDef.isDiscard) "[Discard] " else ""
            prefix + node.subDef.filter.ifBlank { "(all transactions)" }
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
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions", "transaction-row-menu-${nodeName(node)}") {
            addItem("Edit") { editNode(node) }
            if (node is TxNode.DefNode) {
                addItem("Add policy specialization") { addSpec(node.def) }
            }
            addItem("Delete") { deleteNode(node) }
        }

    private fun editNode(node: TxNode) = when (node) {
        is TxNode.DefNode -> editDef(node.def)
        is TxNode.SubNode -> editSpec(node.parent, node.subDef)
    }

    private fun deleteNode(node: TxNode) = when (node) {
        is TxNode.DefNode -> confirm("Delete transaction", "Delete \"${node.def.displayName}\"?", "Delete") {
            defs().remove(node.def)
            changed()
        }

        is TxNode.SubNode -> confirm("Delete specialization", "Delete this policy specialization?", "Delete") {
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
        TransactionType.MATCHED -> "Match individual methods or entire classes by name, configured directly in the UI without touching your code."
        TransactionType.DECLARED -> "Declare transactions in your source code with the @MethodTransaction and @ClassTransaction annotations."
        TransactionType.MAPPED -> "Map any existing annotation such as @Service or @Transactional to a transaction without modifying your code."
        else -> ""
    }

    sealed class TxNode {
        class DefNode(val def: TransactionDef) : TxNode()
        class SubNode(val subDef: PolicySubDef, val parent: TransactionDef) : TxNode()
    }
}
