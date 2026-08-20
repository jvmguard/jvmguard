package dev.jvmguard.ui.shell

import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.Locales
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import dev.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.menubar.MenuBar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LanguageSelectTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.ADMIN)))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    @Test
    fun selectorOffersAutoAndAllSupportedLocales() {
        navigate(VmsView::class.java)
        val select = find<MenuBar>().all().first { it.testId == LanguageSelect.ID }
        val items = select.items.single().subMenu.items
        assertEquals(listOf(LanguageSelect.ID_AUTO) + Locales.SUPPORTED.map { "${LanguageSelect.ID}-${Locales.tag(it)}" },
            items.map { it.testId })
        assertEquals("Auto (English)", items[0].text)
    }

    @Test
    fun autoIsCheckedByDefault() {
        navigate(VmsView::class.java)
        val items = find<MenuBar>().all().first { it.testId == LanguageSelect.ID }.items.single().subMenu.items
        assertEquals(listOf(true, false, false, false, false), items.map { it.isChecked })
    }

    @Test
    fun storedLocaleIsAppliedAtUiInit() {
        Sessions.current()!!.viewSettings.locale = "ja"
        val ui = UI()
        ui.locale = Locales.ENGLISH // the browser-detected locale
        Locales.initUiLocale(ui)
        assertEquals(Locales.JAPANESE, ui.locale, "the user's stored choice overrides the detected locale")
        assertEquals(Locales.ENGLISH, Locales.detectedLocale(ui), "the detected locale is captured for the Auto label")
    }

    @Test
    fun emptyStoredLocaleKeepsTheDetectedLocale() {
        val ui = UI()
        ui.locale = Locales.KOREAN
        Locales.initUiLocale(ui)
        assertEquals(Locales.KOREAN, ui.locale)
        assertNull(Locales.storedLocale())
    }

    @Test
    fun selectingALanguagePersistsIt() {
        navigate(VmsView::class.java)
        LanguageSelect().select("zh-CN")
        assertEquals("zh-CN", Sessions.current()!!.viewSettings.locale)
        assertNotNull(Locales.storedLocale())
    }

    @Test
    fun storedLocaleIsAppliedWhenLoginCreatesTheSession() {
        // The UI exists before login
        val connection = MockConnections.create(AccessLevel.ADMIN)
        connection.user.viewSettings.locale = "ja"
        UI.getCurrent().locale = Locales.KOREAN
        Sessions.setCurrent(UserSession(connection))
        assertEquals(Locales.JAPANESE, UI.getCurrent().locale)
    }
}
