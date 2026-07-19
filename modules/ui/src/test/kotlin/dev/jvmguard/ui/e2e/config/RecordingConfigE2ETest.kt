package dev.jvmguard.ui.e2e.config

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.WaitUntilState
import dev.jvmguard.ui.components.recording.AbstractTransactionDefDialog
import dev.jvmguard.ui.components.recording.telemetries.TelemetryConfigDialog
import dev.jvmguard.ui.components.recording.telemetries.TelemetryGrid
import dev.jvmguard.ui.components.recording.thresholds.ThresholdDialog
import dev.jvmguard.ui.components.recording.thresholds.ThresholdGrid
import dev.jvmguard.ui.components.recording.triggers.TriggerDialog
import dev.jvmguard.ui.components.recording.triggers.TriggerGrid
import org.junit.jupiter.api.Test

/**
 * Recording routes use settings mode, so the recording Save is the shared `MainLayout.ID_SETTINGS_SAVE`
 * button driven by [applySettings]. On a fresh server all entities land on the root group.
 */
class RecordingConfigE2ETest : ConfigE2ETest() {

    @Test
    fun telemetryPersists() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openRecording("recording/telemetries")
        getByTestId(TelemetryGrid.ID_GRID).waitFor()
        getByTestId("telemetry-add").click()
        getByTestId(TelemetryConfigDialog.ID_NAME).locator("input").fill("Heap usage")
        getByTestId(TelemetryConfigDialog.ID_SAVE).click()
        // The grid prompts "Add a line?"; the telemetry is already added, so decline.
        getByText("Cancel").last().click()
        applySettings()

        openRecording("recording/telemetries")
        assertThat(getByTestId(TelemetryGrid.ID_GRID).getByText("Heap usage")).isVisible()
    }

    @Test
    fun thresholdPersists() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openRecording("recording/thresholds")
        getByTestId(ThresholdGrid.ID_GRID).waitFor()
        getByTestId("threshold-add").click()
        getByTestId(ThresholdDialog.ID_TELEMETRY).click()
        val firstType = getByRole(AriaRole.OPTION).first()
        firstType.waitFor()
        val telemetryName = firstType.innerText().trim()
        firstType.click()
        getByTestId(ThresholdDialog.ID_LOWER_ENABLED).locator("input").check()
        getByTestId(ThresholdDialog.ID_LOWER_VALUE).locator("input").fill("80")
        getByTestId(ThresholdDialog.ID_SAVE).click()
        applySettings()

        openRecording("recording/thresholds")
        assertThat(getByTestId(ThresholdGrid.ID_GRID).getByText(telemetryName)).isVisible()
    }

    @Test
    fun triggerPersists() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openRecording("recording/triggers")
        getByTestId(TriggerGrid.ID_GRID).waitFor()
        // A connection-count trigger has no threshold prerequisite.
        getByTestId("trigger-add").click()
        getByText("Connection count trigger").click()
        getByTestId(TriggerDialog.ID_SAVE).waitFor()
        getByTestId(TriggerDialog.ID_SAVE).click()
        applySettings()

        openRecording("recording/triggers")
        assertThat(getByTestId("trigger-row-menu-0")).isVisible()
    }

    @Test
    fun matchedTransactionPersists() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openRecording("recording/transactions")
        locator("vaadin-tab").filter(Locator.FilterOptions().setHasText("Matched")).first().click()
        getByTestId("transaction-grid-matched").waitFor()
        getByTestId("transaction-add").click()
        getByTestId(AbstractTransactionDefDialog.ID_SAVE).waitFor()
        // The "Class or interface name" field has no test id; reach it by its label.
        locator("vaadin-text-field")
            .filter(Locator.FilterOptions().setHasText("Class or interface name"))
            .first().locator("input").fill("com.example.OrderService")
        getByTestId(AbstractTransactionDefDialog.ID_SAVE).click()
        applySettings()

        openRecording("recording/transactions")
        locator("vaadin-tab").filter(Locator.FilterOptions().setHasText("Matched")).first().click()
        getByTestId("transaction-grid-matched").waitFor()
        // A class-only Matched def is displayed as "<class>#*".
        assertThat(getByTestId("transaction-grid-matched").getByText("com.example.OrderService#*")).isVisible()
    }

    private fun Page.openRecording(route: String) {
        navigate("$baseUrl/$route", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
    }
}
