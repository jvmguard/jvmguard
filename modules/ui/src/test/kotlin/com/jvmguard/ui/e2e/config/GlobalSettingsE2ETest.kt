package com.jvmguard.ui.e2e.config

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.jvmguard.ui.views.settings.DataSettingsView
import com.jvmguard.ui.views.settings.DisplaySettingsView
import org.junit.jupiter.api.Test

class GlobalSettingsE2ETest : ConfigE2ETest() {

    @Test
    fun transactionCapPersists() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openGeneralSettings("settings/data")
        val capInput = getByTestId(DataSettingsView.ID_TRANSACTION_CAP).locator("input")
        capInput.fill("4242")
        applySettings()

        openGeneralSettings("settings/data")
        assertThat(getByTestId(DataSettingsView.ID_TRANSACTION_CAP).locator("input")).hasValue("4242")
    }

    @Test
    fun customWindowTitlePersists() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openGeneralSettings("settings/display")
        getByTestId(DisplaySettingsView.ID_TITLE_ENABLED).locator("input").check()
        getByTestId(DisplaySettingsView.ID_TITLE_TEXT).locator("input").fill("JvmGuard QA")
        applySettings()

        openGeneralSettings("settings/display")
        assertThat(getByTestId(DisplaySettingsView.ID_TITLE_ENABLED).locator("input")).isChecked()
        assertThat(getByTestId(DisplaySettingsView.ID_TITLE_TEXT).locator("input")).hasValue("JvmGuard QA")
    }
}
