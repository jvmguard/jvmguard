package dev.jvmguard.ui.components

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.icon.VaadinIcon

fun menuButton(
    icon: VaadinIcon,
    ariaLabel: String,
    testId: String,
    tooltip: String = ariaLabel,
    build: ContextMenu.() -> Unit,
): Button {
    val button = Button(icon.create()).apply {
        addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL)
        setAriaLabel(ariaLabel)
        setTooltipText(tooltip)
        this.testId = testId
    }
    ContextMenu().apply {
        target = button
        isOpenOnClick = true
        build()
    }
    return button
}
