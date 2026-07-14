package dev.jvmguard.ui.views.settings

import dev.jvmguard.connector.server.mock.MockServerConnectionImpl
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SsoViewTest : JvmGuardBrowserlessTest() {

    private lateinit var connection: MockServerConnectionImpl

    @BeforeEach
    fun setUp() {
        connection = MockConnections.create(AccessLevel.ADMIN)
        Sessions.setCurrent(UserSession(connection))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.clearSettingsDraft()
    }

    private fun textField(label: String): TextField = find<TextField>().all().first { it.label == label }
    private fun passwordField(label: String): PasswordField = find<PasswordField>().all().first { it.label == label }
    private fun button(text: String): Button = find<Button>().all().first { it.text == text }
    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }
    private fun dialogSave(): Button = find<Button>().all().first { it.text == "Save" && "jvmguard-settings-save" !in it.classNames }

    @Test
    fun addingAProviderPersistsOnSave() {
        UI.getCurrent().navigate(SsoView::class.java)
        use(button("Add provider")).click()

        use(textField("Display name")).setValue("Company Okta")
        assertTrue(
            textField("Authorized redirect URI").value.endsWith("/login/oauth2/code/company-okta"),
            "the redirect URI reflects the display-name slug",
        )
        use(textField("Issuer URI")).setValue("https://yourco.okta.com")
        use(textField("Client ID")).setValue("client-123")
        use(passwordField("Client secret")).setValue("secret-456")
        use(textField("Domain / tenant")).setValue("yourco.com")
        use(dialogSave()).click()

        assertTrue(shellSave().isEnabled, "staging a provider enables Save")
        assertTrue(connection.getGlobalConfig(false).ssoConfig.providers.isEmpty(), "not committed before Save")

        use(shellSave()).click()

        val providers = connection.getGlobalConfig(false).ssoConfig.providers
        assertEquals(1, providers.size)
        assertEquals("Company Okta", providers[0].displayName)
        assertEquals("https://yourco.okta.com", providers[0].issuerUri)
        assertEquals("client-123", providers[0].clientId)
        assertEquals("secret-456", providers[0].clientSecret)
        assertEquals("yourco.com", providers[0].domainRestriction)
    }

    @Test
    fun providerWithEmptyRulesGridIsClosedByDefault() {
        UI.getCurrent().navigate(SsoView::class.java)
        use(button("Add provider")).click()

        use(textField("Display name")).setValue("Google")
        use(textField("Issuer URI")).setValue("https://accounts.google.com")
        use(textField("Client ID")).setValue("client-id")
        use(passwordField("Client secret")).setValue("secret")
        use(textField("Domain / tenant")).setValue("yourco.com")
        use(dialogSave()).click()

        use(shellSave()).click()

        val provider = connection.getGlobalConfig(false).ssoConfig.providers[0]
        assertTrue(provider.accessRules.isEmpty(), "no rules = closed circle")
    }

    @Test
    fun addingAProviderWithoutClientIdAndSecretPersistsOnSave() {
        UI.getCurrent().navigate(SsoView::class.java)
        use(button("Add provider")).click()

        use(textField("Display name")).setValue("Env Var Okta")
        use(textField("Issuer URI")).setValue("https://yourco.okta.com")
        use(dialogSave()).click()

        assertTrue(shellSave().isEnabled, "client ID and secret are optional")

        use(shellSave()).click()

        val providers = connection.getGlobalConfig(false).ssoConfig.providers
        assertEquals(1, providers.size)
        assertEquals("Env Var Okta", providers[0].displayName)
        assertEquals("", providers[0].clientId, "client ID is blank, to be injected via env var")
        assertEquals("", providers[0].clientSecret, "client secret is blank, to be injected via env var")
    }

    @Test
    fun nonAdminIsForwardedAwayFromSsoSettings() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.VIEWER)))
        UI.getCurrent().navigate(SsoView::class.java)

        assertFalse(SsoView::class.java.isInstance(currentView))
    }
}
