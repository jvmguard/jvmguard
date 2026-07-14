package dev.jvmguard.ui.e2e.screenshots

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import dev.jvmguard.ui.views.data.transactions.TransactionsView
import org.junit.jupiter.api.Test

class TransactionsScreenshots : ScreenshotTest() {

    @Test
    fun hotspots() = onPage {
        login(demo = true)
        open("transactions?${vmQuery(DEMO_VM)}")
        getByTestId(TransactionsView.ID_GRID).waitFor()
        getByTestId(TransactionsView.ID_MODE).getByText("Hot spots").click()
        getByTestId(TransactionsView.ID_GRID).waitFor()
        capture("hotspots")
    }

    @Test
    fun calltreeTransactionTimeline() = onPage {
        login(demo = true)
        open("transactions?${vmQuery(DEMO_VM)}")
        getByTestId(TransactionsView.ID_GRID).waitFor()
        assertThat(locator("jvmguard-echart canvas").first()).isVisible()
        capture("calltree_transaction_timeline")
    }

    @Test
    fun exportButton() = onPage {
        login(demo = true)
        open("transactions?${vmQuery(DEMO_VM)}")
        getByTestId(TransactionsView.ID_GRID).waitFor()
        assertThat(getByTestId(TransactionsView.ID_EXPORT)).isVisible()
        getByTestId(TransactionsView.ID_EXPORT).capture("export_button")
    }
}
