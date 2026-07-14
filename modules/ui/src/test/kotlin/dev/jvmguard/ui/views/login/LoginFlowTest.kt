package dev.jvmguard.ui.views.login

import dev.jvmguard.connector.api.SsoProviderInfo
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.DefaultLoginService
import dev.jvmguard.ui.server.LoginService
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.QueryParameters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginFlowTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setLoginService(object : LoginService {
            override fun login(userName: String, password: String, authenticatorCode: String?) = MockConnections.create()
            override fun isUse2fa() = false
            override fun enabledSsoProviders() = emptyList<SsoProviderInfo>()
        })
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.setLoginService(DefaultLoginService())
    }

    @Test
    fun unauthenticatedRootForwardsToLogin() {
        UI.getCurrent().navigate("")
        assertInstanceOf(LoginView::class.java, currentView)
    }

    @Test
    fun mockFlagSurvivesTheForwardToLogin() {
        // The forward to login must keep the mock query, otherwise the login connects to the real backend.
        UI.getCurrent().navigate("", QueryParameters.simple(mapOf("mock" to "")))
        assertInstanceOf(LoginView::class.java, currentView)
        assertTrue(Sessions.isMockRequested())
    }

    @Test
    fun noMockFlagWhenAbsentFromTheUrl() {
        UI.getCurrent().navigate("")
        assertInstanceOf(LoginView::class.java, currentView)
        assertFalse(Sessions.isMockRequested())
    }

    @Test
    fun successfulLoginRoutesToVmsView() {
        val login = navigate(LoginView::class.java)

        use(login.userName).setValue("admin")
        use(login.password).setValue("secret")
        use(login.loginButton).click()

        assertTrue(Sessions.isLoggedIn())
        assertInstanceOf(VmsView::class.java, currentView)
    }

}
