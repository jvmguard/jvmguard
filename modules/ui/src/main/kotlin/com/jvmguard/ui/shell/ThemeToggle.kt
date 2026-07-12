package com.jvmguard.ui.shell

import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.page.ColorScheme
import com.vaadin.flow.component.page.Page
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
            applyEffectiveScheme(page)
        } else {
            page.colorScheme = DEFAULT_COLOR_SCHEME
            page.executeJs("return window.matchMedia('(prefers-color-scheme: dark)').matches")
                .then(Boolean::class.javaObjectType) { dark ->
                    effectiveDark = dark == true
                    updateIcon()
                    applyEffectiveScheme(page)
                }
        }
    }

    private fun toggle() {
        val next = if (effectiveDark) ColorScheme.Value.LIGHT else ColorScheme.Value.DARK
        VaadinSession.getCurrent().setAttribute(COLOR_SCHEME_ATTRIBUTE, next)
        val page = UI.getCurrent().page
        page.colorScheme = next
        effectiveDark = next == ColorScheme.Value.DARK
        updateIcon()
        applyEffectiveScheme(page)
    }

    // Page.colorScheme alone is not enough. The CSS color-scheme property can't switch a background-image, and the `[theme~="dark"]`
    // attribute is only set for an explicit dark scheme
    private fun applyEffectiveScheme(page: Page) {
        page.executeJs(
            "document.documentElement.setAttribute('jvmguard-scheme', \$0 ? 'dark' : 'light')",
            effectiveDark,
        )
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
