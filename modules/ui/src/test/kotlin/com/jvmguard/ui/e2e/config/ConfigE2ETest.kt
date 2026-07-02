package com.jvmguard.ui.e2e.config

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.e2e.PlaywrightE2ETest
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag

/**
 * Base for the real-server configuration E2E tests. These mutate shared server state, so teardown
 * restores the canonical `test` admin and the `config-e2e` tag keeps them out of the `?mock` smoke tests.
 */
@Tag("config-e2e")
abstract class ConfigE2ETest : PlaywrightE2ETest() {

    @AfterEach
    fun restoreCanonicalAdmin() {
        controlCommand("reset")
        controlCommand("createUser")
    }

    protected fun freshServer() {
        controlCommand("reset")
        controlCommand("createUser")
    }

    protected fun Page.loginReal(user: String = "test", password: String = "password4329", authCode: String? = null) {
        navigate("$baseUrl/login", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill(user)
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill(password)
        authCode?.let { getByTestId(LoginView.ID_AUTHCODE).locator("input").fill(it) }
        getByTestId(LoginView.ID_SUBMIT).click()
    }

    protected fun Page.loginRealAndWaitForApp(user: String = "test", password: String = "password4329", authCode: String? = null) {
        loginReal(user, password, authCode)
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }

    protected fun Page.logout() {
        getByTestId(MainLayout.ID_USER_MENU).click()
        getByTestId(MainLayout.ID_LOGOUT).click()
        getByTestId(LoginView.ID_USERNAME).waitFor()
    }

    protected fun Page.openGeneralSettings(route: String) {
        getByTestId(MainLayout.ID_SETTINGS).click()
        getByTestId(MainLayout.ID_GENERAL_SETTINGS).click()
        navigate("$baseUrl/$route", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
    }

    protected fun Page.applySettings() {
        getByTestId(MainLayout.ID_SETTINGS_SAVE).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }

}
