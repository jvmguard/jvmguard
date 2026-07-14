package dev.jvmguard.ui.e2e.screenshots

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitUntilState
import dev.jvmguard.ui.views.setup.InstallWizardView
import org.junit.jupiter.api.Test

/** Empties the DB so the server reports a fresh install, then restores the shared `test` user for later tests. */
class WizardScreenshots : ScreenshotTest() {

    @Test
    fun installerUser() = onPage {
        controlCommand("reset")
        try {
            navigate("$baseUrl/login", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
            getByTestId(InstallWizardView.ID_NAME).waitFor()
            capture("installer_user")
        } finally {
            controlCommand("reset")
            controlCommand("createUser")
        }
    }
}
