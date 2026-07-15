package dev.jvmguard.ui.components

import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.icon.VaadinIcon

private const val OPEN_MENU_KEY = "dev.jvmguard.ui.components.openContextMenu"

fun anyContextMenuOpen(): Boolean = UI.getCurrent()?.let { openMenuIn(it) } != null

private fun openMenuIn(ui: UI): ContextMenu? =
    ComponentUtil.getData(ui, OPEN_MENU_KEY) as? ContextMenu

private fun setOpenMenuIn(ui: UI, menu: ContextMenu?) {
    ComponentUtil.setData(ui, OPEN_MENU_KEY, menu)
}

fun ContextMenu.trackSingleOpen() {
    val ui = UI.getCurrent() ?: return
    addOpenedChangeListener { e ->
        if (e.isOpened) {
            val previous = openMenuIn(ui)
            setOpenMenuIn(ui, this)
            previous?.takeIf { it !== this }?.close()
        } else if (openMenuIn(ui) === this) {
            setOpenMenuIn(ui, null)
        }
    }
}

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
        trackSingleOpen()
        build()
    }
    return button
}
