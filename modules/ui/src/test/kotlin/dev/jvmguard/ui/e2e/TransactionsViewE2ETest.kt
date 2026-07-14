package dev.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import dev.jvmguard.ui.views.data.VmDataView
import dev.jvmguard.ui.views.data.transactions.TransactionNavigationBar
import dev.jvmguard.ui.views.data.transactions.TransactionTimeLinePanel
import dev.jvmguard.ui.views.data.transactions.TransactionsView
import dev.jvmguard.ui.views.login.LoginView
import dev.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("e2e")
class TransactionsViewE2ETest : PlaywrightE2ETest() {

    @Test
    fun rendersTransactionTreeAndTimelineForASingleVm() = onPage {
        openTransactions("Database/DB 01")

        assertThat(getByTestId(TransactionsView.ID_GRID)).isVisible()
        assertThat(getByTestId(VmDataView.ID_SELECT_BUTTON)).isVisible()
        assertThat(getByText("Database").first()).isVisible()
        assertThat(getByText("Request 1").first()).isVisible()
        assertThat(locator("${TransactionTimeLinePanel.ID_CHART.testIdSelector()} canvas").first()).isVisible()
        assertThat(getByTestId(TransactionNavigationBar.ID_PREVIOUS)).isVisible()

        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("transactions.png")))
    }

    @Test
    fun expandingATransaction_revealsChildMethods() = onPage {
        openTransactions("Database/DB 01")

        locator("vaadin-grid-tree-toggle").first().click()
        assertThat(getByText("Child 1").first()).isVisible()
    }

    @Test
    fun filter_reducesTheVisibleTransactions() = onPage {
        openTransactions("Database/DB 01")

        val input = getByTestId(TransactionsView.ID_FILTER).locator("input")
        input.click()
        input.pressSequentially("Request 2")
        input.blur() // commit the LAZY value-change

        // vaadin-grid recycles cell DOM, so filtered-out rows linger as hidden elements: assert on
        // visibility, not DOM count.
        assertThat(getByText("Request 2").first()).isVisible()
        assertThat(getByText("Request 1").first()).isHidden()
    }

    @Test
    fun rowMenu_pinsAPerTransactionTimeLine() = onPage {
        openTransactions("Database/DB 01")

        getByTestId(TransactionsView.ID_ROW_MENU).first().click()
        getByText("Show time line of total times").click()

        assertThat(getByTestId(TransactionTimeLinePanel.ID_CLOSE)).isVisible()
        assertThat(locator("${TransactionTimeLinePanel.ID_CHART.testIdSelector()} canvas").first()).isVisible()
    }

    @Test
    fun modeTabs_switchToHotSpots_relabelsTheColumn() = onPage {
        openTransactions("Database/DB 01")

        getByText("Hot spots").click()
        assertThat(getByText("Hot spot", Page.GetByTextOptions().setExact(true)).first()).isVisible()
        assertThat(getByTestId(TransactionsView.ID_GRID)).isVisible()
        assertThat(getByTestId(TransactionsView.ID_ROW_MENU).first()).isVisible()
    }

    @Test
    fun overdueMode_hidesTheTimeLinePanel() = onPage {
        openTransactions("Database/DB 01")

        assertThat(getByTestId(TransactionTimeLinePanel.ID_CHART)).isVisible()
        getByText("Overdue").click()
        assertThat(getByTestId(TransactionTimeLinePanel.ID_CHART)).isHidden()
        assertThat(getByTestId(TransactionsView.ID_ROW_MENU)).hasCount(0)
        assertThat(getByTestId(TransactionsView.ID_GRID)).isVisible()
    }

    @Test
    fun statusFilterMenu_filtersByPolicy() = onPage {
        openTransactions("Database/DB 01")

        // "Error" matches nothing in the mock data, so the empty-state message shows.
        getByTestId(TransactionsView.ID_STATUS_FILTER).click()
        assertThat(getByText("Very slow")).isVisible()
        getByText("Error").click()
        assertThat(getByText("No transactions match the current filter.")).isVisible()
    }

    @Test
    fun mobileViewport_stacksAndScrollsInsteadOfSplitting() = onPage {
        setViewportSize(420, 740)
        openTransactions("Database/DB 01")

        val split = locator("vaadin-split-layout.jvmguard-transaction-split")
        assertThat(split).isVisible()

        val overflowY = split.evaluate("el => getComputedStyle(el).overflowY") as String
        assertEquals("auto", overflowY)

        val primaryHeight = (split.evaluate(
            "el => el.querySelector('[slot=\"primary\"]').getBoundingClientRect().height"
        ) as Number).toDouble()
        assertTrue(primaryHeight > 300.0, "primary pane should keep its fixed height, was $primaryHeight")

        assertThat(getByTestId(TransactionsView.ID_GRID)).isVisible()
        assertThat(locator("${TransactionTimeLinePanel.ID_CHART.testIdSelector()} canvas").first()).isVisible()
    }

    private fun String.testIdSelector(): String = "[data-testid='$this']"

    private fun Page.openTransactions(vmPath: String) {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()

        val encoded = vmPath.replace(" ", "%20")
        navigate("$baseUrl/transactions?vm=$encoded", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(TransactionsView.ID_GRID).waitFor()
    }
}
