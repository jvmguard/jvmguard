package dev.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitUntilState
import dev.jvmguard.ui.views.login.LoginView
import dev.jvmguard.ui.views.setup.InstallWizardView
import dev.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * `reset` empties the DB so the server reports a fresh install; teardown must restore the shared
 * server's canonical `test` user for the other E2E classes.
 */
@Tag("e2e")
class WizardE2ETest : PlaywrightE2ETest() {

    @Test
    fun freshInstallRunsWizardAndCreatesAdmin() = onPage {
        controlCommand("reset")
        try {
            navigate("$baseUrl/login", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
            getByTestId(InstallWizardView.ID_NAME).waitFor()
            screenshot(Page.ScreenshotOptions().setPath(screenshotPath("install-wizard.png")))

            getByTestId(InstallWizardView.ID_NAME).locator("input").fill("wizardadmin")
            getByTestId(InstallWizardView.ID_2FA).click() // uncheck to skip 2FA enrollment
            getByTestId(InstallWizardView.ID_PASSWORD).locator("input").fill("wizardpass")
            getByTestId(InstallWizardView.ID_CONFIRM).locator("input").fill("wizardpass")
            getByTestId(InstallWizardView.ID_SUBMIT).click()

            getByTestId(VmTreeGrid.ID_GRID).waitFor()
        } finally {
            controlCommand("reset")
            controlCommand("createUser")
        }
    }

    @Test
    fun installedServerShowsLoginNotWizard() = onPage {
        navigate("$baseUrl/login", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).waitFor()
    }
}
