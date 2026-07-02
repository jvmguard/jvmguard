package com.jvmguard.ui.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.MouseButton
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.views.data.telemetry.TelemetryNavigationBar
import com.jvmguard.ui.views.data.telemetry.VmTelemetryView
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("e2e")
class VmTelemetryViewE2ETest : PlaywrightE2ETest() {

    @Test
    fun singleVm_rendersChartWithControls() = onPage {
        openTelemetry("Database/DB 01")

        assertThat(getByTestId(VmTelemetryView.ID_CHART)).isVisible()
        assertThat(locator("jvmguard-echart canvas").first()).isVisible()

        for (control in listOf(
            VmTelemetryView.ID_TYPE_SELECT,
            TelemetryNavigationBar.ID_INTERVAL,
            TelemetryNavigationBar.ID_ZOOM_OUT,
            VmTelemetryView.ID_EXPORT,
        )) {
            assertThat(getByTestId(control)).isVisible()
        }

        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("telemetry-view.png")))
    }

    @Test
    fun navigationOverlay_pagesBackwardThenRevealsForward() = onPage {
        openTelemetry("Database/DB 01")
        assertThat(locator("jvmguard-echart canvas").first()).isVisible()

        assertThat(locator("jvmguard-echart .nav-backward")).hasCount(1)
        assertThat(locator("jvmguard-echart .nav-forward")).hasCount(0)

        locator("jvmguard-echart .nav-backward").click()
        assertThat(locator("jvmguard-echart .nav-forward")).hasCount(1)
    }

    @Test
    fun contextMenuZoomOutHere_widensTheInterval() = onPage {
        openTelemetry("Database/DB 01")
        assertThat(locator("jvmguard-echart canvas").first()).isVisible()

        locator("jvmguard-echart").click(Locator.ClickOptions().setButton(MouseButton.RIGHT))
        getByText("Zoom out here").click()

        assertThat(getByTestId(TelemetryNavigationBar.ID_INTERVAL)).containsText("20 minutes")
    }

    @Test
    fun logarithmicYAxis_rendersViaContextMenu() = onPage {
        openTelemetry("Database/DB 01")
        assertThat(locator("jvmguard-echart canvas").first()).isVisible()

        locator("jvmguard-echart").click(Locator.ClickOptions().setButton(MouseButton.RIGHT))
        getByText("Logarithmic Y-axis").click()

        assertThat(locator("jvmguard-echart canvas").first()).isVisible()
        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("telemetry-log.png")))
    }

    @Test
    fun customTelemetry_rendersFromTheTypeSelect() = onPage {
        openTelemetry("Database/DB 01")
        assertThat(locator("jvmguard-echart canvas").first()).isVisible()

        // Exercises the getCustomTelemetryData path end to end.
        getByTestId(VmTelemetryView.ID_TYPE_SELECT).click()
        getByText("Request queue length").click()

        assertThat(locator("jvmguard-echart canvas").first()).isVisible()
    }

    @Test
    fun group_rendersAggregateChart() = onPage {
        openTelemetry("Database")

        assertThat(locator("jvmguard-echart canvas").first()).isVisible()
    }

    @Test
    fun switchingTelemetryType_keepsChartRendered() = onPage {
        openTelemetry("Database/DB 01")
        assertThat(locator("jvmguard-echart canvas").first()).isVisible()

        getByTestId(VmTelemetryView.ID_TYPE_SELECT).click()
        getByText("Heap Usage").first().click()

        assertThat(locator("jvmguard-echart canvas").first()).isVisible()
    }

    private fun Page.openTelemetry(vmPath: String) {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()

        val encoded = vmPath.replace(" ", "%20")
        navigate("$baseUrl/telemetry?vm=$encoded", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(VmTelemetryView.ID_TYPE_SELECT).waitFor()
    }
}
