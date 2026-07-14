package dev.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import dev.jvmguard.ui.views.data.VmDataView
import dev.jvmguard.ui.views.data.telemetry.TelemetryOverviewPanel
import dev.jvmguard.ui.views.data.telemetry.VmTelemetryView
import dev.jvmguard.ui.views.login.LoginView
import dev.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

@Tag("e2e")
class TelemetryOverviewE2ETest : PlaywrightE2ETest() {

    @Test
    fun rendersGridOfSparklinesForASingleVm() = onPage {
        openOverview("Database/DB 01")

        val overviewGrid = getByTestId(TelemetryOverviewPanel.ID_GRID)
        assertThat(overviewGrid).isVisible()
        assertThat(getByTestId(VmDataView.ID_SELECT_BUTTON)).isVisible()
        assertThat(getByText("Database").first()).isVisible()
        // Scoped to the grid: these names also exist as hidden items of the single-telemetry type select.
        assertThat(overviewGrid.getByText("Heap size").first()).isVisible()
        assertThat(overviewGrid.getByText("CPU load").first()).isVisible()
        assertThat(overviewGrid.getByText("Fake telemetry 1").first()).isVisible()
        assertThat(locator("jvmguard-sparkline").first()).isVisible()

        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("telemetry-overview.png")))
    }

    @Test
    fun overviewMode_hidesTheSingleTelemetryControls() = onPage {
        openOverview("Database/DB 01")

        assertThat(getByTestId(VmTelemetryView.ID_TYPE_SELECT)).isHidden()
        assertThat(getByTestId(VmTelemetryView.ID_CHART)).isHidden()
    }

    @Test
    fun clickingASparkline_switchesToTheSingleTelemetryMode() = onPage {
        openOverview("Database/DB 01")
        assertThat(locator("jvmguard-sparkline").first()).isVisible()

        locator("jvmguard-sparkline.jvmguard-sparkline-link").first().click()

        assertThat(this).hasURL(Pattern.compile(".*/telemetry\\?.*t=.*"))
        assertFalse(url().contains("vm="), "the VM selection must not appear in the URL")
        assertThat(locator("jvmguard-echart canvas").first()).isVisible()
        assertThat(getByTestId(TelemetryOverviewPanel.ID_GRID)).isHidden()
    }

    @Test
    fun rendersAggregateForAGroup() = onPage {
        openOverview("Database")

        assertThat(getByTestId(TelemetryOverviewPanel.ID_GRID)).isVisible()
        assertThat(locator("jvmguard-sparkline").first()).isVisible()
    }

    @Test
    fun selectorDialog_expandsAllJvmsByDefault() = onPage {
        // At the root selection no group name leaks onto the page, so a visible "Database" can only
        // come from the expanded dialog tree.
        openOverview("")

        getByTestId(VmDataView.ID_SELECT_BUTTON).click()
        assertThat(locator("vaadin-dialog-overlay")).isVisible()
        assertThat(getByText("Database", Page.GetByTextOptions().setExact(true)).first()).isVisible()
    }

    @Test
    fun selectorDialog_confirmsTheSelectionWithEnter() = onPage {
        openOverview("Database/DB 01")

        getByTestId(VmDataView.ID_SELECT_BUTTON).click()
        val overlay = locator("vaadin-dialog-overlay[opened]")
        assertThat(overlay).isVisible()

        // Press Enter on the overlay, not the page body: the Select button's Enter shortcut is
        // registered with listenOn(dialog), so the keydown must land inside the dialog's scope.
        overlay.press("Enter")
        assertThat(overlay).isHidden()
    }

    private fun Page.openOverview(vmPath: String) {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()

        val encoded = vmPath.replace(" ", "%20")
        navigate("$baseUrl/telemetry?vm=$encoded", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(VmTelemetryView.ID_MODE).getByText("Overview").click()
        getByTestId(TelemetryOverviewPanel.ID_GRID).waitFor()
    }
}
