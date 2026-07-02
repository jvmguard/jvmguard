package com.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.account.AccountProfileView
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.settings.DataSettingsView
import com.jvmguard.ui.views.settings.UsersView
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("e2e")
class SettingsE2ETest : PlaywrightE2ETest() {

    @Test
    fun cogEntersSettingsAndSwitchesSections() = onPage {
        login()

        getByTestId(MainLayout.ID_SETTINGS).click()
        getByTestId(MainLayout.ID_GENERAL_SETTINGS).click()
        assertThat(getByTestId(UsersView.ID_GRID)).isVisible()
        assertThat(getByTestId(MainLayout.ID_SETTINGS_SAVE)).isVisible()

        getByText("Data retention").click()
        assertThat(getByTestId(DataSettingsView.ID_TRANSACTION_CAP)).isVisible()

        getByTestId(MainLayout.ID_SETTINGS_CANCEL).click()
        assertThat(getByTestId(VmTreeGrid.ID_GRID)).isVisible()
    }

    @Test
    fun accountOpensFromTheHeaderMenu() = onPage {
        login()

        getByTestId(MainLayout.ID_USER_MENU).click()
        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("user-menu.png")))
        getByTestId(MainLayout.ID_ACCOUNT).click()

        assertThat(getByTestId(AccountProfileView.ID_FULL_NAME)).isVisible()
    }

    private fun Page.login() {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }
}
