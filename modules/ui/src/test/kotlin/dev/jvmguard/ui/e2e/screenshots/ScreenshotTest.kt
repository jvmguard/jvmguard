package dev.jvmguard.ui.e2e.screenshots

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.TimeoutError
import com.microsoft.playwright.options.WaitUntilState
import dev.jvmguard.ui.e2e.PlaywrightE2ETest
import dev.jvmguard.ui.views.login.LoginView
import dev.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Tag

/**
 * Each test writes a PNG named exactly like the help figure (see `modules/docs/agent/help-screenshots.md`); the
 * `:jvmguard:web:screenshots` task (tag `screenshot`) feeds `build/e2e/screenshotsLight` into the help PDF.
 * Prefer [Locator.capture] (cropped) over [Page.capture] so figures stay tight; full page only for whole-view shots.
 */
@Tag("screenshot")
abstract class ScreenshotTest : PlaywrightE2ETest() {

    /**
     * Logs in and waits for the VM grid. The dev-mode server occasionally serves the first request slowly, so a login
     * that does not reach the grid within [LOGIN_GRID_TIMEOUT_MS] is retried from scratch (up to [LOGIN_ATTEMPTS]),
     * which makes the screenshot tasks deterministic instead of flaking ~1 test per full run.
     */
    protected fun Page.login(demo: Boolean = false) {
        val mock = if (demo) "?mock=demo" else "?mock"
        var lastError: TimeoutError? = null
        repeat(LOGIN_ATTEMPTS) {
            try {
                navigate("$baseUrl/login$mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
                getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
                getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
                getByTestId(LoginView.ID_SUBMIT).click()
                getByTestId(VmTreeGrid.ID_GRID).waitFor(Locator.WaitForOptions().setTimeout(LOGIN_GRID_TIMEOUT_MS))
                return
            } catch (e: TimeoutError) {
                lastError = e
            }
        }
        throw lastError!!
    }

    protected fun Page.open(routeWithQuery: String) {
        navigate("$baseUrl/$routeWithQuery", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
    }

    protected fun vmQuery(vmPath: String): String = "vm=" + vmPath.replace(" ", "%20")

    /**
     * Representative demo VM for the per-VM data screenshots
     */
    protected companion object {
        const val DEMO_VM: String = "Demo/Purchase/Checkout"

        private const val LOGIN_ATTEMPTS: Int = 3
        private const val LOGIN_GRID_TIMEOUT_MS: Double = 15_000.0
    }

    protected fun Page.capture(name: String) {
        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("$name${darkSuffix()}.png")))
    }

    protected fun Locator.capture(name: String) {
        screenshot(Locator.ScreenshotOptions().setPath(screenshotPath("$name${darkSuffix()}.png")))
    }

    private fun darkSuffix(): String = if (darkScreenshots) "_dark" else ""

}
