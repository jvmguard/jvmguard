package com.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.components.recording.telemetries.TelemetryConfigDialog
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Vaadin's dialog overlay marks `[part=content]` as `flex: 1; overflow: auto`, so at fractional
 * device-pixel ratios the content height rounds a sub-pixel short and draws a phantom scrollbar.
 */
@Tag("e2e")
class DialogScrollbarE2ETest : PlaywrightE2ETest() {

    @Test
    fun simpleDialogHasNoContentScrollbarAtAnyScale() {
        for (scale in listOf(1.0, 1.1, 1.25, 1.33, 1.5, 1.75, 2.0)) {
            val width = scrollbarWidthAt(scale)
            println("[DialogScrollbarE2ETest] dpr=$scale content scrollbar width = ${width}px")
            assertTrue(width <= 0.0, "Dialog content shows a ${width}px vertical scrollbar at devicePixelRatio $scale")
        }
    }

    private fun scrollbarWidthAt(scale: Double): Double {
        var width = 0.0
        onPage(deviceScaleFactor = scale) {
            login()
            navigate("$baseUrl/recording/telemetries?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
            getByTestId("telemetry-add").click()
            getByTestId(TelemetryConfigDialog.ID_NAME).waitFor()
            width = contentScrollbarWidth()
            if (scale == 1.5) {
                screenshot(Page.ScreenshotOptions().setPath(screenshotPath("telemetry-dialog-scrollbar.png")))
            }
        }
        return width
    }

    /**
     * `offsetWidth - clientWidth` catches a sub-pixel overflow the browser still draws a scrollbar for,
     * which a `scrollHeight - clientHeight` check (integer-rounded) would miss.
     */
    private fun Page.contentScrollbarWidth(): Double {
        val overlay = locator("vaadin-dialog-overlay").last()
        return (overlay.evaluate(
            "el => { const c = el.shadowRoot.querySelector('[part=\"content\"]'); return c.offsetWidth - c.clientWidth; }",
        ) as Number).toDouble()
    }

    private fun Page.login() {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }
}
