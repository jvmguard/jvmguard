package com.jvmguard.ui.views.setup

import com.jvmguard.connector.api.SsoProviderInfo
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.*
import com.jvmguard.ui.views.login.LoginView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InstallWizardTest : JvmGuardBrowserlessTest() {

    private lateinit var setup: FakeSetupService

    @BeforeEach
    fun setUp() {
        setup = FakeSetupService(fresh = true)
        Sessions.setSetupService(setup)
        // The base navigates once before this runs, caching the default service's answer; clear it so the fake is consulted.
        Sessions.resetNewInstallationCache()
        Sessions.setLoginService(object : LoginService {
            override fun login(userName: String, password: String, authenticatorCode: String?) = MockConnections.create()
            override fun isUse2fa() = false
            override fun enabledSsoProviders() = emptyList<SsoProviderInfo>()
        })
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.setSetupService(DefaultSetupService())
        Sessions.setLoginService(DefaultLoginService())
    }

    private fun submit() = use(find<Button>().all().first { it.testId == InstallWizardView.ID_SUBMIT }).click()
    private fun fieldValue(testId: String, value: String) {
        use(find<TextField>().all().first { it.testId == testId }).setValue(value)
    }

    private fun passwordValue(testId: String, value: String) {
        use(find<PasswordField>().all().first { it.testId == testId }).setValue(value)
    }

    @Test
    fun freshInstallForwardsLoginToWizard() {
        UI.getCurrent().navigate(LoginView::class.java)
        assertInstanceOf(InstallWizardView::class.java, currentView)
    }

    @Test
    fun installedForwardsWizardToLogin() {
        setup.fresh = false
        UI.getCurrent().navigate(InstallWizardView::class.java)
        assertInstanceOf(LoginView::class.java, currentView)
    }

    @Test
    fun completingWizardCreatesAdminAndLogsIn() {
        UI.getCurrent().navigate(InstallWizardView::class.java)
        fieldValue(InstallWizardView.ID_NAME, "admin")
        passwordValue(InstallWizardView.ID_PASSWORD, "secret1")
        passwordValue(InstallWizardView.ID_CONFIRM, "secret1")
        submit()

        assertEquals(1, setup.createCount)
        assertEquals("admin", setup.createdName)
        assertEquals(true, setup.createdUse2fa)
        assertTrue(Sessions.isLoggedIn())
    }

    @Test
    fun passwordMismatchBlocksSubmit() {
        UI.getCurrent().navigate(InstallWizardView::class.java)
        fieldValue(InstallWizardView.ID_NAME, "admin")
        passwordValue(InstallWizardView.ID_PASSWORD, "secret1")
        passwordValue(InstallWizardView.ID_CONFIRM, "different")
        submit()

        assertEquals(0, setup.createCount)
        assertTrue(find<TextField>().all().any { it.testId == InstallWizardView.ID_NAME })
    }

    private class FakeSetupService(var fresh: Boolean) : SetupService {
        var createdName: String? = null
        var createdUse2fa: Boolean? = null
        var createCount = 0

        override fun isNewInstallation() = fresh

        override fun createInitialUser(
            userName: String,
            fullName: String,
            email: String,
            passwordHash: String,
            use2fa: Boolean,
            groupConfig: GroupConfig,
        ) {
            createdName = userName
            createdUse2fa = use2fa
            createCount++
            fresh = false
        }
    }
}
