package com.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.log.ConnectionLogView
import com.jvmguard.ui.views.log.ServerLogView
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * The e2e harness server configures no log4j file appenders (a real jar install does, via `log4j2.xml`
 * next to it), so there are no log-file descriptors to assert against here; the view's data path is
 * covered by [com.jvmguard.ui.views.log.LogViewTest].
 */
@Tag("e2e")
class ServerLogE2ETest : PlaywrightE2ETest() {

    @Test
    fun loggingModeOpensFromCogAndSwitchesLogs() = onPage {
        login()

        getByTestId(MainLayout.ID_SETTINGS).click()
        getByTestId(MainLayout.ID_LOGS).click()

        assertThat(getByTestId(ServerLogView.ID)).isVisible()
        assertThat(getByTestId(MainLayout.ID_LOG_CLOSE)).isVisible()
        assertThat(getByTestId(MainLayout.ID_LOG_NAV_SERVER)).isVisible()
        assertThat(getByTestId(MainLayout.ID_LOG_NAV_CONNECTION)).isVisible()
        assertThat(getByTestId(MainLayout.ID_LOG_NAV_EVENT)).isVisible()
        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("logging-mode.png")))

        getByTestId(MainLayout.ID_LOG_NAV_CONNECTION).click()
        assertThat(getByTestId(ConnectionLogView.ID)).isVisible()

        getByTestId(MainLayout.ID_LOG_CLOSE).click()
        assertThat(getByTestId(VmTreeGrid.ID_GRID)).isVisible()
    }

    private fun Page.login() {
        navigate("$baseUrl/login", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }
}
