package com.jvmguard.ui.components.recording.telemetries

import com.jvmguard.agent.config.VmType
import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.components.JvmGuardDialog
import com.jvmguard.ui.components.showCentered
import com.jvmguard.ui.components.showFilling
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.findVm
import com.jvmguard.ui.views.data.VmBreadcrumb
import com.jvmguard.ui.views.data.VmSelectorDialog
import com.jvmguard.ui.views.data.mbeans.AttributeNode
import com.jvmguard.ui.views.data.mbeans.MBeanBrowserPanel
import com.jvmguard.ui.views.data.mbeans.PathValidationHelper
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout

class NumericMBeanValueDialog(
    private val onPick: (objectName: String, attributePath: String, suggestedName: String) -> Unit,
) : JvmGuardDialog() {

    private var selectedIdentifier: VmIdentifier? = null
    private var selected: AttributeNode? = null

    private val panel = MBeanBrowserPanel(::onAttributeSelected, ::onAttributeConfirmed)
    private val parentOf: (AttributeNode) -> AttributeNode? = { panel.parentOf(it) }
    private val breadcrumb = VmBreadcrumb(::select)
    private val message = Span().apply { addClassName("jvmguard-field-hint") }
    private val placeholder = VerticalLayout(
        Span("Please select a single named VM or a VM pool first."),
        Button("Select a VM", VaadinIcon.SEARCH.create()) { openSelector() }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
        },
    ).apply {
        isPadding = false
        defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
        style.set("gap", "0.75rem")
    }
    private val contentHost = VerticalLayout().apply {
        setSizeFull()
        isPadding = false
        isSpacing = false
        style.set("min-height", "0")
    }

    private val okButton = Button("Select") { confirm() }.apply {
        addThemeVariants(ButtonVariant.PRIMARY)
        isEnabled = false
        testId = ID_CONFIRM
    }

    init {
        headerTitle = "Select an MBean value"
        width = "70rem"
        height = "44rem"

        val selectButton = Button(VaadinIcon.SEARCH.create()) { openSelector() }.apply {
            addThemeVariants(ButtonVariant.TERTIARY)
            setAriaLabel("Select VM pool or VM")
            setTooltipText("Select VM pool or VM")
            testId = ID_SELECT_VM
        }
        val toolbar = HorizontalLayout(selectButton, breadcrumb).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isPadding = false
            setWidthFull()
            isWrap = true
            setFlexGrow(1.0, breadcrumb)
        }
        add(VerticalLayout(toolbar, contentHost, message).apply {
            setSizeFull()
            isPadding = false
            isSpacing = true
            setFlexGrow(1.0, contentHost)
        })
        footer.add(Button("Cancel") { close() }, okButton)
        contentHost.showCentered(placeholder)
    }

    private fun openSelector() = VmSelectorDialog(
        selectedIdentifier ?: VmIdentifier.ROOT_GROUP_IDENTIFIER,
        ::select,
        { it.type != VmType.GROUP },
        "Select VM pool or VM",
    ).open()

    private fun select(identifier: VmIdentifier) {
        selectedIdentifier = identifier
        breadcrumb.setSelection(identifier)
        resetSelection()
        val vm = resolveVm(identifier)
        if (vm == null) {
            contentHost.showCentered(placeholder)
        } else {
            contentHost.showFilling(panel)
            panel.setVm(vm)
        }
    }

    private fun resolveVm(identifier: VmIdentifier): VM? {
        val connection = Sessions.current()?.serverConnection ?: return null
        val vm = connection.findVm(identifier) ?: return null
        return if (identifier.type == VmType.POOL) {
            connection.getConnectedPooledVms(vm).firstOrNull()
        } else {
            vm
        }
    }

    private fun onAttributeSelected(node: AttributeNode?) {
        selected = node
        val error = if (node == null) null else PathValidationHelper.validatePath(node, parentOf)
        message.text = error ?: ""
        okButton.isEnabled = node != null && error == null
    }

    private fun onAttributeConfirmed(node: AttributeNode) {
        selected = node
        if (PathValidationHelper.validatePath(node, parentOf) == null) {
            confirm()
        }
    }

    private fun resetSelection() {
        selected = null
        message.text = ""
        okButton.isEnabled = false
    }

    private fun confirm() {
        val node = selected ?: return
        val objectName = panel.selectedObjectName ?: return
        if (PathValidationHelper.validatePath(node, parentOf) != null) {
            return
        }
        val path = PathValidationHelper.getSelectedPath(node, parentOf)
        onPick(objectName, path, PathValidationHelper.getSuggestedLineName(path))
        close()
    }

    companion object {
        const val ID_SELECT_VM = "mbean-picker-vm"
        const val ID_CONFIRM = "mbean-picker-confirm"
    }
}
