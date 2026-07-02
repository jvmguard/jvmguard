package com.jvmguard.ui.views.data

import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.shell.CachedView
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.QueryParameters
import com.vaadin.flow.router.RouteConfiguration
import com.vaadin.flow.shared.Registration

abstract class VmDataView : VerticalLayout(), BeforeEnterObserver, CachedView {

    private val breadcrumb = VmBreadcrumb(::select)

    protected val content = VerticalLayout().apply {
        setSizeFull()
        isPadding = true
        isSpacing = false
    }

    protected var currentSelection: VmIdentifier = VmIdentifier.ROOT_GROUP_IDENTIFIER
        private set

    private var modelRegistration: Registration? = null
    private var pollRegistration: Registration? = null
    private var lastRenderedSelection: VmIdentifier? = null

    protected var selectionRendered = false
        private set

    // Deep-link state
    private var pendingSeed: VmIdentifier? = null
    private var seededFromUrl = false
    private var entryQuery: QueryParameters = QueryParameters.empty()

    init {
        setSizeFull()
        isPadding = false
        isSpacing = false

        val selectButton = Button(VaadinIcon.SEARCH.create()) { openSelector() }.apply {
            addThemeVariants(ButtonVariant.TERTIARY)
            testId = ID_SELECT_BUTTON
            setAriaLabel("Select group or JVM")
            setTooltipText("Select group or JVM")
        }

        val toolbar = HorizontalLayout(selectButton, breadcrumb).apply {
            addClassName("jvmguard-data-toolbar")
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isPadding = true
            style.set("padding-block-end", "0")
            setWidthFull()
            isWrap = true
            setFlexGrow(1.0, breadcrumb)
        }

        add(toolbar, content)
        setFlexGrow(1.0, content)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        selectionRendered = false
        val params = event.location.queryParameters
        if (params.parameters.containsKey(VmSelection.PARAM)) {
            pendingSeed = VmSelection.fromQuery(params)
            seededFromUrl = true
            entryQuery = params
            if (isAttached) {
                applyPendingSeed()
            }
        } else {
            seededFromUrl = false
        }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        selectionRendered = false
        val model = Sessions.vmSelectionModel()
        modelRegistration = model.addListener(::applySelection)
        applyPendingSeed()
        applySelection(model.selection)
        pollRegistration = attachEvent.ui.addPollListener { onPollTick() }
    }

    private fun applyPendingSeed() {
        val seed = pendingSeed ?: return
        pendingSeed = null
        Sessions.vmSelectionModel().set(seed)
    }

    override fun onDetach(detachEvent: DetachEvent) {
        pollRegistration?.remove()
        pollRegistration = null
        modelRegistration?.remove()
        modelRegistration = null
        super.onDetach(detachEvent)
    }

    private fun applySelection(selection: VmIdentifier) {
        currentSelection = selection
        breadcrumb.setSelection(selection)
        // Reload only when the selection actually changed — otherwise a plain view switch keeps the
        // retained view's state in place.
        if (selection != lastRenderedSelection) {
            lastRenderedSelection = selection
            selectionRendered = true
            onSelectionChanged(selection)
        }
    }

    /** Called on first render and whenever the selected group/JVM actually changes. */
    protected abstract fun onSelectionChanged(selection: VmIdentifier)

    protected open fun onPollTick() {}

    protected fun openSelector() =
        VmSelectorDialog(currentSelection, ::select, ::isSelectable, selectorTitle()).open()

    protected open fun isSelectable(selection: VmIdentifier): Boolean = true

    protected open fun selectorTitle(): String = "Select group or JVM"

    private fun select(selection: VmIdentifier) {
        val model = Sessions.vmSelectionModel()
        if (selection == model.selection) {
            return
        }
        if (seededFromUrl) {
            stripVmFromUrl()
            seededFromUrl = false
        }
        model.set(selection)
    }

    private fun stripVmFromUrl() {
        val params = LinkedHashMap(entryQuery.parameters)
        params.remove(VmSelection.PARAM)
        val path = RouteConfiguration.forSessionScope().getUrl(javaClass)
        val query = QueryParameters(params).queryString
        val location = if (query.isEmpty()) path else "$path?$query"
        ui.ifPresent { it.page.history.replaceState(null, location) }
    }

    companion object {
        const val ID_SELECT_BUTTON = "vm-select-button"
    }
}
