package com.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.components.recording.telemetries.TelemetryConfigDialog
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("e2e")
class DialogScrollbarE2ETest : PlaywrightE2ETest() {

    @Test
    fun autoSizedDialogDoesNotClipLastFieldAtAnyScale() {
        for (scale in listOf(1.0, 1.1, 1.25, 1.33, 1.5, 1.75, 2.0)) {
            onPage(deviceScaleFactor = scale) {
                login()
                openAddTelemetryDialog()
                val m = measureContent()
                println("[DialogScrollbarE2ETest] dpr=$scale overflow=${m.overflow} lastFieldGap=${m.lastFieldGap}")
                assertEquals(
                    0.0, m.overflow,
                    "Dialog content is clipped by ${m.overflow}px (scrollHeight-clientHeight) at devicePixelRatio $scale",
                )
                assertTrue(
                    m.scrollbarWidth <= 0.0,
                    "Dialog content shows a ${m.scrollbarWidth}px scrollbar at devicePixelRatio $scale",
                )
                assertTrue(
                    m.lastFieldGap >= 1.0,
                    "Last field sits flush against the scroll boundary (gap ${m.lastFieldGap}px) at devicePixelRatio $scale",
                )
                if (scale == 1.5) {
                    screenshot(Page.ScreenshotOptions().setPath(screenshotPath("telemetry-dialog-scrollbar.png")))
                }
            }
        }
    }

    private data class ContentMetrics(val overflow: Double, val scrollbarWidth: Double, val lastFieldGap: Double)

    private fun Page.measureContent(): ContentMetrics {
        val overlay = locator("vaadin-dialog-overlay").last()
        @Suppress("UNCHECKED_CAST")
        val m = overlay.evaluate(
            """
            el => {
              const content = el.shadowRoot.querySelector('[part="content"]');
              const host = el.getRootNode().host;
              const boxes = host.querySelectorAll('vaadin-checkbox');
              const last = boxes[boxes.length - 1];
              const cr = content.getBoundingClientRect();
              const clientBottom = cr.top + content.clientHeight;
              return {
                overflow: content.scrollHeight - content.clientHeight,
                scrollbarWidth: content.offsetWidth - content.clientWidth,
                lastFieldGap: last ? clientBottom - last.getBoundingClientRect().bottom : 999,
              };
            }
            """.trimIndent(),
        ) as Map<String, Any>
        return ContentMetrics(
            (m["overflow"] as Number).toDouble(),
            (m["scrollbarWidth"] as Number).toDouble(),
            (m["lastFieldGap"] as Number).toDouble(),
        )
    }

    private fun Page.openAddTelemetryDialog() {
        navigate("$baseUrl/recording/telemetries?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId("telemetry-add").click()
        getByTestId(TelemetryConfigDialog.ID_NAME).waitFor()
    }

    private fun Page.login() {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }
}
