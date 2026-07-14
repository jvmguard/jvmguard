package dev.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import dev.jvmguard.ui.views.login.LoginView
import dev.jvmguard.ui.views.settings.UsersView
import dev.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Logs in against the real backend (not `?mock`): `?mock` swaps in the mock connection's user, but we
 * need the granted authorities of the actual logged-in user.
 */
@Tag("e2e")
class RoleAccessE2ETest : PlaywrightE2ETest() {

    @Test
    fun viewerIsDeniedAdminViewWhileAdminReachesIt() {
        controlCommand("createUser&accessLevel=VIEWER&name=$VIEWER_USER&password=$VIEWER_PASSWORD")

        onPage {
            login("test", "password4329")
            navigate("$baseUrl/settings/users", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
            assertThat(getByTestId(UsersView.ID_GRID)).isVisible()
        }

        onPage {
            login(VIEWER_USER, VIEWER_PASSWORD)
            assertThat(getByTestId(VmTreeGrid.ID_GRID)).isVisible()

            navigate("$baseUrl/settings/users", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
            assertThat(getByTestId(UsersView.ID_GRID)).not().isVisible()
        }
    }

    @Test
    fun backendMethodSecurityDeniesViewerAndAllowsAdmin() {
        // 403 (AccessDeniedException) rather than 400 confirms @PreAuthorize is enforcing on the
        // backend ServerConnection, beyond the route guards.
        assertEquals(403, controlCommand("checkBackendAuthz&accessLevel=VIEWER"))
        assertEquals(200, controlCommand("checkBackendAuthz&accessLevel=ADMIN"))
    }

    private fun Page.login(user: String, password: String) {
        navigate("$baseUrl/login", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill(user)
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill(password)
        getByTestId(LoginView.ID_SUBMIT).click()
        // Wait for the VMs view before navigating, so the security context is established first.
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }

    private companion object {
        const val VIEWER_USER = "viewer"
        const val VIEWER_PASSWORD = "viewerpass"
    }
}
