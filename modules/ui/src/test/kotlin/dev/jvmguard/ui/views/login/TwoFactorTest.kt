package dev.jvmguard.ui.views.login

import dev.jvmguard.connector.api.SsoProviderInfo
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.components.TwoFactorEnroller
import dev.jvmguard.ui.server.*
import dev.jvmguard.ui.views.vms.VmsView
import dev.jvmguard.connector.server.mock.MockServerConnectionImpl
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TwoFactorTest : JvmGuardBrowserlessTest() {

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.setLoginService(DefaultLoginService())
    }

    private fun connectionWith2fa(): MockServerConnectionImpl = MockConnections.create(AccessLevel.ADMIN).also {
        val config = it.getGlobalConfig(false)
        config.use2fa = true
        it.setGlobalConfig(config)
    }

    private fun loginService(use2fa: Boolean, connection: () -> MockServerConnectionImpl = { MockConnections.create() }) =
        object : LoginService {
            override fun login(userName: String, password: String, authenticatorCode: String?) = connection()
            override fun isUse2fa() = use2fa
            override fun enabledSsoProviders() = emptyList<SsoProviderInfo>()
        }

    @Test
    fun authenticatorFieldShownOnlyWhenGlobal2faOn() {
        Sessions.setLoginService(loginService(use2fa = true))
        assertTrue(LoginView().authCode.isVisible, "the field shows when 2FA is on")
        Sessions.setLoginService(loginService(use2fa = false))
        assertFalse(LoginView().authCode.isVisible, "the field is hidden when 2FA is off")
    }

    @Test
    fun loginRoutesToForcedSetupWhenEnrollmentRequired() {
        val connection = connectionWith2fa()
        Sessions.setLoginService(loginService(use2fa = true) { connection })
        UI.getCurrent().navigate(LoginView::class.java)
        val login = find<LoginView>().single()
        use(login.userName).setValue("admin")
        use(login.password).setValue("secret")
        use(login.loginButton).click()

        // The mock user is not enrolled and not exempt, so enrollment is forced before the data views.
        assertInstanceOf(AccountSetupView::class.java, currentView)
    }

    @Test
    fun shellForwardsToSetupWhileEnrollmentPending() {
        Sessions.setCurrent(UserSession(connectionWith2fa()))
        UI.getCurrent().navigate(VmsView::class.java)
        assertInstanceOf(AccountSetupView::class.java, currentView)
    }

    @Test
    fun enrollerRejectsAnIncorrectCode() {
        val enroller = TwoFactorEnroller("admin")
        enroller.codeField.value = "000000"
        assertNull(enroller.verifiedSecretHex(), "a wrong code does not verify")
        assertTrue(enroller.codeField.isInvalid)
    }

    @Test
    fun noForcedSetupWhenGlobal2faOff() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.ADMIN)))
        UI.getCurrent().navigate(VmsView::class.java)
        assertInstanceOf(VmsView::class.java, currentView)
        assertFalse(Sessions.current()!!.forcedSetupRequired())
    }
}
