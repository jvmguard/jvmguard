package com.jvmguard.ui.e2e.config

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.settings.UserEditDialog
import com.jvmguard.ui.views.settings.UsersView
import org.junit.jupiter.api.Test

class UsersE2ETest : ConfigE2ETest() {

    @Test
    fun addedAdminUserPersistsAndCanLogIn() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openGeneralSettings("settings/users")
        getByTestId(UsersView.ID_GRID).waitFor()
        assertThat(getByTestId("${UsersView.ID_ROW_MENU}-test")).isVisible()

        addUser("DummyAdmin", "dummyadmin@test.com", "passwordAdmin4329", "Admin")
        applySettings()

        logout()
        loginRealAndWaitForApp("DummyAdmin", "passwordAdmin4329")
        getByTestId(MainLayout.ID_SETTINGS).click()
        assertThat(getByTestId(MainLayout.ID_GENERAL_SETTINGS)).isVisible()
    }

    @Test
    fun viewerAccessLevelGatesConfiguration() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openGeneralSettings("settings/users")
        getByTestId(UsersView.ID_GRID).waitFor()
        addUser("DummyViewer", "dummyviewer@test.com", "passwordViewer4329", "Viewer")
        applySettings()

        logout()
        loginRealAndWaitForApp("DummyViewer", "passwordViewer4329")
        assertThat(getByTestId(MainLayout.ID_SETTINGS)).hasCount(0)
    }

    private fun Page.addUser(login: String, email: String, password: String, accessLevel: String) {
        getByTestId(UsersView.ID_ADD).click()
        getByTestId(UserEditDialog.ID_LOGIN_NAME).locator("input").fill(login)
        getByTestId(UserEditDialog.ID_EMAIL).locator("input").fill(email)
        getByTestId(UserEditDialog.ID_ACCESS_LEVEL).click()
        // Match the dropdown OPTION by role: the same label also appears as a grid cell.
        getByRole(AriaRole.OPTION, Page.GetByRoleOptions().setName(accessLevel).setExact(true)).click()
        getByTestId(UserEditDialog.ID_PASSWORD).locator("input").fill(password)
        getByTestId(UserEditDialog.ID_CONFIRM_PASSWORD).locator("input").fill(password)
        getByTestId(UserEditDialog.ID_SAVE).click()
    }
}
