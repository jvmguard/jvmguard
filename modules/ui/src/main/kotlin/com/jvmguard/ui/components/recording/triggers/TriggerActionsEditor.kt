package com.jvmguard.ui.components.recording.triggers

import com.jvmguard.data.config.sets.ActionSet
import com.jvmguard.data.config.triggers.actions.ActionType
import com.jvmguard.data.config.triggers.actions.TriggerAction
import com.jvmguard.ui.components.*
import com.jvmguard.ui.components.recording.actionTypeIcon
import com.jvmguard.ui.components.recording.sets.SetSpec
import com.jvmguard.ui.components.recording.sets.setActionButtons
import com.jvmguard.ui.server.Sessions
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H5
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout

class TriggerActionsEditor(private val actions: MutableList<TriggerAction>) : VerticalLayout() {

    private val header = HorizontalLayout().apply {
        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        isPadding = false
        setWidthFull()
    }

    private var saveSetButton: Button? = null

    private val grid = Grid<TriggerAction>().apply {
        testId = ID_GRID
        addComponentColumn(::actionCell).setHeader("Action").setFlexGrow(1)
        addComponentColumn(::rowActions).setKey(ACTIONS_KEY).setAutoWidth(true).setFlexGrow(0)
        setEmptyStateComponent(Span("No actions yet. Use \"Add action\".").apply { addClassName("jvmguard-field-hint") })
        addItemDoubleClickListener { edit(it.item) }
        editDeleteKeys(::edit, ::remove)
        enableRowReorder(items = { actions }, onReordered = ::refresh)
        setSizeFull()
    }

    init {
        isPadding = false
        isSpacing = true
        setSizeFull()
        val add = dropdownButton("Add action", ID_ADD) {
            ACTION_TYPES.forEach { type -> item(actionTypeIcon(type), type.toString()) { addAction(type) } }
        }
        val setButtons = setActionButtons(setSpec())
        saveSetButton = setButtons.getOrNull(1) as? Button
        header.add(add, *setButtons.toTypedArray())
        add(H5("Actions").apply { addClassName("jvmguard-form-subhead") }, header, grid)
        setFlexGrow(1.0, grid)
        refresh()
    }

    private fun refresh() {
        grid.setItems(actions)
        saveSetButton?.isEnabled = actions.isNotEmpty()
    }

    private fun addAction(type: ActionType) {
        val action = type.createAction()
        TriggerActionDialog.create(action, isNew = true) {
            actions.add(action)
            refresh()
        }.open()
    }

    private fun edit(action: TriggerAction) {
        TriggerActionDialog.create(action, isNew = false) { refresh() }.open()
    }

    private fun actionCell(action: TriggerAction): Component =
        cellRow(actionTypeIcon(action.actionType).create().apply { setSize("1.2em") }, Span(action.description))

    private fun rowActions(action: TriggerAction): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions", "$ID_ROW_MENU-${actions.indexOf(action)}") {
            addItem("Edit") { edit(action) }
            addItem("Remove") { remove(action) }
        }

    private fun remove(action: TriggerAction) {
        actions.remove(action)
        refresh()
    }

    private fun setSpec(): SetSpec<TriggerAction, ActionSet> = SetSpec(
        setClass = ActionSet::class.java,
        singularName = "action set",
        pluralName = "action sets",
        addSubtitle = "The actions in the selected set are added to this trigger.",
        saveSubtitle = "Saved action sets can be added to other triggers.",
        loadSets = {
            Sessions.current()?.serverConnection?.actionSets ?: emptyList()
        },
        currentItems = { actions },
        createSet = { name, items -> ActionSet(name, items) },
        appendItems = { added ->
            actions.addAll(added)
            refresh()
        },
    )

    companion object {
        const val ID_GRID = "trigger-actions-grid"
        const val ID_ADD = "trigger-action-add"
        const val ID_ROW_MENU = "trigger-action-row-menu"
        const val ACTIONS_KEY = "trigger-action-actions"

        private val ACTION_TYPES = ActionType.entries
    }
}
