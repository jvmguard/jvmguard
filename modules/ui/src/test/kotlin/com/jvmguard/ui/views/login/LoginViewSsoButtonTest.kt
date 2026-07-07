package com.jvmguard.ui.views.login

import com.jvmguard.connector.api.SsoProviderInfo
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.DefaultLoginService
import com.jvmguard.ui.server.LoginService
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.MockConnections
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginViewSsoButtonTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setLoginService(object : LoginService {
            override fun login(userName: String, password: String, authenticatorCode: String?) = MockConnections.create()
            override fun isUse2fa() = false
            override fun enabledSsoProviders() = listOf(
                SsoProviderInfo("google", "Google", google = true),
                SsoProviderInfo("okta", "Company Okta"),
            )
        })
    }

    @AfterEach
    fun tearDown() {
        Sessions.setLoginService(DefaultLoginService())
    }

    private fun ssoButtons() = find<Button>().all().filter { "jvmguard-sso-button" in it.classNames }

    @Test
    fun ssoButtonsRenderWhenProvidersConfigured() {
        val view = LoginView()
        UI.getCurrent().add(view)

        val buttons = ssoButtons()
        assertEquals(2, buttons.size, "expected 2 SSO buttons but found ${buttons.size}")
    }

    @Test
    fun noSsoButtonsWhenProvidersNotConfigured() {
        Sessions.setLoginService(object : LoginService {
            override fun login(userName: String, password: String, authenticatorCode: String?) = MockConnections.create()
            override fun isUse2fa() = false
            override fun enabledSsoProviders() = emptyList<SsoProviderInfo>()
        })

        val view = LoginView()
        UI.getCurrent().add(view)

        assertTrue(ssoButtons().isEmpty(), "no SSO buttons when no providers")
    }
}
