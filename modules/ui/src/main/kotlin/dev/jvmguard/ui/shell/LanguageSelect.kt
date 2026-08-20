package dev.jvmguard.ui.shell

import dev.jvmguard.ui.server.Locales
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.contextmenu.SubMenu
import com.vaadin.flow.component.icon.SvgIcon
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.menubar.MenuBarVariant

class LanguageSelect : MenuBar() {

    init {
        testId = ID
        addClassName("jvmguard-collapsible")
        addClassName("jvmguard-language-select")
        addThemeVariants(MenuBarVariant.LUMO_TERTIARY, MenuBarVariant.LUMO_ICON)
        val root = addItem(SvgIcon("icons/translate.svg").apply { setSize("1.25rem") }).apply {
            element.setAttribute("aria-label", t("language.selector.aria"))
        }
        val stored = Locales.storedLocale()
        root.subMenu.apply {
            addLanguageItem(t("language.auto", Locales.nativeName(Locales.detectedLocale(UI.getCurrent()))), "", stored == null, ID_AUTO)
            addSeparator()
            Locales.SUPPORTED.forEach { locale ->
                val tag = Locales.tag(locale)
                addLanguageItem(Locales.nativeName(locale), tag, stored == locale, "$ID-$tag")
            }
        }
    }

    private fun SubMenu.addLanguageItem(label: String, tag: String, selected: Boolean, itemTestId: String) {
        addItem(label) {
            select(tag)
            // Rebuild all views in the new locale; the stored choice is applied at UI init after reload.
            UI.getCurrent().page.reload()
        }.apply {
            testId = itemTestId
            isCheckable = true
            isChecked = selected
        }
    }

    internal fun select(tag: String) {
        Sessions.current()?.let { session ->
            if (session.viewSettings.locale != tag) {
                session.viewSettings.locale = tag
                session.saveViewSettings()
            }
        }
    }

    companion object {
        const val ID = "language-select"
        const val ID_AUTO = "$ID-auto"
    }
}
