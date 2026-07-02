package com.jvmguard.ui.views.settings.recording

import com.jvmguard.agent.config.base.Identifiable
import com.jvmguard.data.config.sets.AbstractSet
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.components.recording.RecordingGrid
import com.jvmguard.ui.components.recording.sets.SetSpec
import com.jvmguard.ui.components.recording.sets.setActionButtons
import com.jvmguard.ui.server.Sessions
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon

abstract class AbstractRecordingListView<T : Identifiable, S : AbstractSet<T>> : AbstractRecordingSettingsView() {

    private var grid: RecordingGrid? = null
    private var currentItems: MutableList<T>? = null
    private var saveSetButton: Button? = null

    protected abstract fun items(selection: VmIdentifier): MutableList<T>?

    protected abstract fun createGrid(items: () -> MutableList<T>, markChanged: () -> Unit): RecordingGrid

    protected abstract val addButtonText: String
    protected abstract val addButtonTestId: String

    protected abstract val setClass: Class<S>
    protected abstract val singularSetName: String
    protected abstract val pluralSetName: String
    protected abstract val addSetSubtitle: String
    protected abstract val saveSetSubtitle: String
    protected abstract fun loadSets(): Collection<S>
    protected abstract fun newSet(name: String, items: List<T>): S

    final override fun onSelectionChanged(selection: VmIdentifier) {
        val items = items(selection)
        content.removeAll()
        if (items == null) {
            grid = null
            currentItems = null
            return
        }
        currentItems = items
        intro(selection)?.let { content.add(Span(it).apply { addClassName("jvmguard-field-hint") }) }
        val newGrid = createGrid({ items }, ::markChanged)
        grid = newGrid
        content.add(newGrid)
        buildToolbar(newGrid)
    }

    protected open fun onChanged() {}

    protected open fun intro(selection: VmIdentifier): String? = null

    protected open fun createAddControl(grid: RecordingGrid): Component =
        Button(addButtonText, VaadinIcon.PLUS.create()) { grid.addNew() }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = addButtonTestId
        }

    override fun onEditableChanged(editable: Boolean) {
        content.isEnabled = editable
    }

    private fun markChanged() {
        onChanged()
        Sessions.recordingDraft().markChanged(Sessions.recordingGroupSelection().selection)
        saveSetButton?.isEnabled = currentItems?.isNotEmpty() == true
    }

    private fun buildToolbar(grid: RecordingGrid) {
        val setButtons = setActionButtons(setSpec(grid))
        saveSetButton = setButtons.getOrNull(1) as? Button
        setToolbarActions(createAddControl(grid), *setButtons.toTypedArray())
    }

    private fun setSpec(grid: RecordingGrid): SetSpec<T, S> {
        val items = currentItems ?: mutableListOf()
        return SetSpec(
            setClass = setClass,
            singularName = singularSetName,
            pluralName = pluralSetName,
            addSubtitle = addSetSubtitle,
            saveSubtitle = saveSetSubtitle,
            loadSets = {
                loadSets()
            },
            currentItems = { items },
            createSet = ::newSet,
            appendItems = { added ->
                items.addAll(added)
                markChanged()
                grid.refresh()
            },
        )
    }
}
