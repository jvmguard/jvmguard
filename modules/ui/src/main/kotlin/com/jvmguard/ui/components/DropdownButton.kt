package com.jvmguard.ui.components

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.contextmenu.SubMenu
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.menubar.MenuBarVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

class DropdownMenu internal constructor(private val menu: SubMenu) {
    fun item(icon: VaadinIcon, label: String, onClick: () -> Unit) {
        menu.addItem(iconLabel(icon, label)) { onClick() }
    }
}

fun dropdownButton(text: String, testId: String, build: DropdownMenu.() -> Unit): Component {
    val menuBar = MenuBar().apply {
        addThemeVariants(MenuBarVariant.LUMO_PRIMARY, MenuBarVariant.LUMO_DROPDOWN_INDICATORS)
        this.testId = testId
        addClassName("jvmguard-dropdown-button")
    }
    val root = menuBar.addItem(iconLabel(VaadinIcon.PLUS, text))
    DropdownMenu(root.subMenu).build()
    return menuBar
}

private fun iconLabel(icon: VaadinIcon, label: String): HorizontalLayout =
    HorizontalLayout(icon.create().apply { setSize("1em") }, Span(label)).apply {
        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        isPadding = false
        isSpacing = false
        style.set("gap", "0.5em")
    }
