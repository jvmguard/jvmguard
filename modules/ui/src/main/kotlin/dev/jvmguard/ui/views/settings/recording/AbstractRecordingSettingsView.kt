package dev.jvmguard.ui.views.settings.recording

import dev.jvmguard.agent.config.base.OptionalConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.views.data.VmBreadcrumb
import dev.jvmguard.ui.views.settings.AbstractSettingsPage
import dev.jvmguard.ui.views.settings.SettingsArea
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.shared.Registration

abstract class AbstractRecordingSettingsView : AbstractSettingsPage() {

    override val requiredAccessLevel: AccessLevel get() = AccessLevel.PROFILER
    override val settingsArea: SettingsArea get() = SettingsArea.RECORDING

    protected open val overrideCategory: ((GroupConfig) -> OptionalConfig)? get() = null

    protected open val overrideLabel: String get() = "Override settings for this group"

    private val breadcrumb = VmBreadcrumb(::select)
    private val toolbarActions = HorizontalLayout().apply {
        addClassName("jvmguard-recording-actions")
        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        isPadding = false
    }
    private val overrideCheckbox = Checkbox().apply {
        testId = ID_OVERRIDE
        addClassName("jvmguard-recording-override")
        isVisible = false
        addValueChangeListener { event -> if (event.isFromClient) onOverrideToggled(event.value) }
    }
    protected val content = VerticalLayout().apply {
        isPadding = false
        setWidthFull()
        add(Span("Settings for this category will be added in a later phase.").apply { addClassName("jvmguard-field-hint") })
    }

    private var currentSelection: VmIdentifier = VmIdentifier.ROOT_GROUP_IDENTIFIER
    private var modelRegistration: Registration? = null
    private var built = false

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        if (!built) {
            overrideCheckbox.label = overrideLabel
            val selectButton = Button(VaadinIcon.SEARCH.create()) { openSelector() }.apply {
                addThemeVariants(ButtonVariant.TERTIARY)
                testId = ID_SELECT_BUTTON
                setAriaLabel("Select group")
                setTooltipText("Select group")
            }
            val toolbar = HorizontalLayout(selectButton, breadcrumb, overrideCheckbox, toolbarActions).apply {
                addClassName("jvmguard-data-toolbar")
                addClassName("jvmguard-recording-toolbar")
                defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                isPadding = true
                style.set("padding-block-end", "0")
                setWidthFull()
                isWrap = true
                setFlexGrow(1.0, breadcrumb)
            }
            add(toolbar, content)
            built = true
        }
        val model = Sessions.recordingGroupSelection()
        modelRegistration = model.addListener(::applySelection)
        applySelection(model.selection)
    }

    override fun onDetach(detachEvent: DetachEvent) {
        modelRegistration?.remove()
        modelRegistration = null
        super.onDetach(detachEvent)
    }

    private fun applySelection(selection: VmIdentifier) {
        currentSelection = selection
        breadcrumb.setSelection(selection)
        val category = overrideCategory
        val showOverride = category != null && !selection.isRoot
        overrideCheckbox.isVisible = showOverride
        if (showOverride) {
            overrideCheckbox.value = Sessions.recordingDraft().groupConfig(selection)?.let(category)?.isUsed == true
        }
        onSelectionChanged(selection)
        updateEditable()
    }

    protected fun setToolbarActions(vararg components: Component) {
        toolbarActions.removeAll()
        toolbarActions.add(*components)
    }

    private fun updateEditable() {
        val editable = isEditable()
        toolbarActions.isEnabled = editable
        onEditableChanged(editable)
    }

    protected fun isEditable(): Boolean {
        val selection = Sessions.recordingGroupSelection().selection
        if (selection.isRoot) {
            return true
        }
        val category = overrideCategory ?: return true
        return Sessions.recordingDraft().groupConfig(selection)?.let(category)?.isUsed == true
    }

    protected open fun onEditableChanged(editable: Boolean) {}

    private fun select(selection: VmIdentifier) {
        Sessions.recordingGroupSelection().set(selection)
    }

    private fun openSelector() = RecordingGroupSelectorDialog(currentSelection, ::select).open()

    private fun onOverrideToggled(value: Boolean) {
        val category = overrideCategory ?: return
        val selection = Sessions.recordingGroupSelection().selection
        val draft = Sessions.recordingDraft()
        val groupConfig = draft.groupConfig(selection) ?: return
        category(groupConfig).isUsed = value
        draft.markChanged(selection)
        updateEditable()
    }

    protected open fun onSelectionChanged(selection: VmIdentifier) {}

    companion object {
        const val ID_OVERRIDE = "recording-override"
        const val ID_SELECT_BUTTON = "recording-select-button"
    }
}
