package dev.jvmguard.ui.e2e.screenshots

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.ui.shell.ThemeToggle
import dev.jvmguard.ui.views.account.AccountProfileView
import dev.jvmguard.ui.views.settings.*
import dev.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Test

class SettingsScreenshots : ScreenshotTest() {

    @Test
    fun configMenu() = onPage {
        login()
        getByTestId(MainLayout.ID_SETTINGS).click()
        assertThat(getByTestId(MainLayout.ID_GENERAL_SETTINGS)).isVisible()
        capture("config_menu")
    }

    @Test
    fun generalSettingsMenu() = onPage {
        login()
        getByTestId(MainLayout.ID_SETTINGS).click()
        getByTestId(MainLayout.ID_GENERAL_SETTINGS).click()
        getByTestId(UsersView.ID_GRID).waitFor()
        capture("general_settings_menu")
    }

    @Test
    fun globalSettings() = onPage {
        login()
        open("settings/data")
        getByTestId(DataSettingsView.ID_TRANSACTION_CAP).waitFor()
        capture("global_settings")
    }

    @Test
    fun ldapConfig() = onPage {
        login()
        open("settings/ldap")
        getByTestId(LdapView.ID_URL).waitFor()
        capture("ldap_config")
    }

    @Test
    fun ldapUserMapping() = onPage {
        login()
        open("settings/ldap")
        getByTestId(LdapView.ID_ADD_MAPPING).click()
        locator("vaadin-dialog-overlay").first().waitFor()
        capture("ldap_user_mapping")
    }

    @Test
    fun ssoProvider() = onPage {
        login()
        open("settings/sso")
        getByTestId(SsoView.ID_ADD_PROVIDER).click()
        locator("vaadin-dialog-overlay").first().waitFor()
        getByTestId(SsoProviderDialog.ID_DISPLAY_NAME).locator("input").fill("Keycloak")
        getByTestId(SsoProviderDialog.ID_ISSUER_URI).locator("input").fill("https://keycloak.example.com/realms/main")
        getByTestId(SsoProviderDialog.ID_CLIENT_ID).locator("input").fill("jvmguard")
        getByTestId(SsoProviderDialog.ID_CLIENT_SECRET).locator("input").fill("secret-abc123")
        getByTestId(SsoProviderDialog.ID_DOMAIN).locator("input").fill("example.com")
        waitForTimeout(300.0)
        locator("vaadin-dialog-overlay").first().capture("sso_provider")
    }

    @Test
    fun configExportServer() = onPage {
        login()
        open("settings/import-export")
        getByTestId(ImportExportView.ID_EXPORT).waitFor()
        capture("config_export_server")
    }

    @Test
    fun userDropDown() = onPage {
        login()
        getByTestId(MainLayout.ID_USER_MENU).click()
        assertThat(getByTestId(MainLayout.ID_ACCOUNT)).isVisible()
        capture("user_drop_down")
    }

    @Test
    fun accountSettings() = onPage {
        login()
        open("account/profile")
        getByTestId(AccountProfileView.ID_FULL_NAME).waitFor()
        capture("account_settings")
    }

    @Test
    fun applyDiscard() = onPage {
        login()
        getByTestId(MainLayout.ID_SETTINGS).click()
        getByTestId(MainLayout.ID_GENERAL_SETTINGS).click()
        assertThat(getByTestId(MainLayout.ID_SETTINGS_SAVE)).isVisible()
        assertThat(getByTestId(MainLayout.ID_SETTINGS_CANCEL)).isVisible()
        capture("apply_discard")
    }

    @Test
    fun darkMode() = onPage {
        login()
        getByTestId(ThemeToggle.ID).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
        waitForTimeout(500.0)
        capture("dark_mode")
    }
}
