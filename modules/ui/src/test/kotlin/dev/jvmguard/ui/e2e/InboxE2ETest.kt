package dev.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.ui.views.inbox.InboxMessageDialog
import dev.jvmguard.ui.views.inbox.InboxView
import dev.jvmguard.ui.views.login.LoginView
import dev.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("e2e")
class InboxE2ETest : PlaywrightE2ETest() {

    @Test
    fun inboxOpensWithBadgeMessageDialogAndMultiMode() = onPage {
        login()

        getByText("Inbox").click()
        assertThat(getByTestId(InboxView.ID_GRID)).isVisible()
        assertThat(getByTestId(MainLayout.ID_INBOX_BADGE)).isVisible()
        // Mock seeds a text message item named "Fatal error message".
        assertThat(getByText("Fatal error message").first()).isVisible()
        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("inbox.png")))

        // Double-click runs the row's primary action: view the message.
        getByText("Fatal error message").first().dblclick()
        assertThat(getByTestId(InboxMessageDialog.ID_CLOSE)).isVisible()
        getByTestId(InboxMessageDialog.ID_CLOSE).click()

        getByTestId(InboxView.ID_DELETE_MULTIPLE).click()
        assertThat(getByTestId(InboxView.ID_CANCEL_MULTI)).isVisible()
        getByTestId(InboxView.ID_CANCEL_MULTI).click()
        assertThat(getByTestId(InboxView.ID_CANCEL_MULTI)).isHidden()
    }

    private fun Page.login() {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }
}
