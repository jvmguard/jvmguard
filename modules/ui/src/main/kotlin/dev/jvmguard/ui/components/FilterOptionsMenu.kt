package dev.jvmguard.ui.components

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.icon.VaadinIcon

class FilterOptionsMenu(testId: String, private val onChanged: () -> Unit) {

    var matchCase = false
        private set
    var useRegex = false
        private set
    private var extraActive = false

    val button = Button(VaadinIcon.FILTER.create()).apply {
        addThemeVariants(ButtonVariant.TERTIARY)
        addClassName("jvmguard-field-icon-button")
        setAriaLabel("Filter options")
        setTooltipText("Filter options")
        this.testId = testId
    }

    val menu = ContextMenu().apply {
        target = button
        isOpenOnClick = true
        trackSingleOpen()
    }

    init {
        menu.addItem("Match case") { event ->
            matchCase = event.source.isChecked
            updateFilterIndicator()
            onChanged()
        }.isCheckable = true
        menu.addItem("Regular expression") { event ->
            useRegex = event.source.isChecked
            updateFilterIndicator()
            onChanged()
        }.isCheckable = true
    }

    fun setExtraActive(active: Boolean) {
        extraActive = active
        updateFilterIndicator()
    }

    private fun updateFilterIndicator() {
        button.element.classList.set("jvmguard-filter-active", matchCase || useRegex || extraActive)
    }
}
