package dev.jvmguard.ui.e2e.screenshots

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import dev.jvmguard.ui.views.data.mbeans.MBeansView
import dev.jvmguard.ui.views.data.transactions.TransactionsView
import dev.jvmguard.ui.views.inbox.InboxView
import dev.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Test

class DataViewsScreenshots : ScreenshotTest() {

    @Test
    fun vms() = onPage {
        login(demo = true)
        assertThat(getByTestId(VmTreeGrid.ID_GRID)).isVisible()
        capture("vms")
    }

    @Test
    fun calltree() = onPage {
        login(demo = true)
        open("transactions?${vmQuery(DEMO_VM)}")
        getByTestId(TransactionsView.ID_GRID).waitFor()
        capture("calltree")
    }

    @Test
    fun mbeanBrowser() = onPage {
        login(demo = true)
        open("mbeans?${vmQuery(DEMO_VM)}")
        getByTestId(MBeansView.ID_TREE).waitFor()
        capture("mbean_browser")
    }

    @Test
    fun inbox() = onPage {
        login(demo = true)
        open("inbox")
        getByTestId(InboxView.ID_GRID).waitFor()
        capture("inbox")
    }
}
