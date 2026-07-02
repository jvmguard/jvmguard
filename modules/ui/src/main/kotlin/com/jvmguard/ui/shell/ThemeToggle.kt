package com.jvmguard.ui.shell

import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.page.ColorScheme
import com.vaadin.flow.server.VaadinSession

class ThemeToggle : Button() {

    private var effectiveDark = false

    init {
        testId = ID
        addClickListener { toggle() }
        updateIcon() // refined on attach once the effective scheme is known
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        applyColorScheme(attachEvent.ui)
    }

    private fun applyColorScheme(ui: UI) {
        val stored = storedColorScheme()
        val page = ui.page
        if (stored == ColorScheme.Value.LIGHT || stored == ColorScheme.Value.DARK) {
            page.colorScheme = stored
            effectiveDark = stored == ColorScheme.Value.DARK
            updateIcon()
        } else {
            page.colorScheme = DEFAULT_COLOR_SCHEME
            page.executeJs("return window.matchMedia('(prefers-color-scheme: dark)').matches")
                .then(Boolean::class.javaObjectType) { dark ->
                    effectiveDark = dark == true
                    updateIcon()
                }
        }
    }

    private fun toggle() {
        val next = if (effectiveDark) ColorScheme.Value.LIGHT else ColorScheme.Value.DARK
        VaadinSession.getCurrent().setAttribute(COLOR_SCHEME_ATTRIBUTE, next)
        UI.getCurrent().page.colorScheme = next
        effectiveDark = next == ColorScheme.Value.DARK
        updateIcon()
    }

    private fun updateIcon() {
        icon = (if (effectiveDark) VaadinIcon.SUN_O else VaadinIcon.MOON).create()
        val label = if (effectiveDark) "Switch to light mode" else "Switch to dark mode"
        setAriaLabel(label)
        setTooltipText(label)
    }

    private fun storedColorScheme(): ColorScheme.Value? =
        VaadinSession.getCurrent().getAttribute(COLOR_SCHEME_ATTRIBUTE) as? ColorScheme.Value

    companion object {
        const val ID = "theme-toggle"
        private const val COLOR_SCHEME_ATTRIBUTE = "jvmguard.theme"
        private val DEFAULT_COLOR_SCHEME = ColorScheme.Value.SYSTEM
    }
}
