package dev.jvmguard.ui.e2e.config

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import dev.jvmguard.ui.components.recording.telemetries.TelemetryGrid
import dev.jvmguard.ui.views.data.AbstractVmSelectorDialog
import dev.jvmguard.ui.views.settings.recording.AbstractRecordingSettingsView
import org.junit.jupiter.api.Test

/**
 * The recording group selector lists only existing server groups, and a fresh E2E server (no agent
 * connects) has just the root group, so a non-root group, and thus the per-group override toggle, cannot
 * be produced here. The non-root override toggle/persist needs a seeded child group.
 */
class VmGroupE2ETest : ConfigE2ETest() {

    @Test
    fun groupSelectorListsRootAndHidesOverrideForRoot() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        navigate("$baseUrl/recording/telemetries", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(TelemetryGrid.ID_GRID).waitFor()

        assertThat(getByTestId(AbstractRecordingSettingsView.ID_SELECT_BUTTON)).isVisible()
        // The per-group override checkbox is hidden for the root selection; it appears only for a non-root group.
        assertThat(getByTestId(AbstractRecordingSettingsView.ID_OVERRIDE)).hasCount(0)
        getByTestId(AbstractRecordingSettingsView.ID_SELECT_BUTTON).click()
        assertThat(getByTestId(AbstractVmSelectorDialog.ID_SELECT)).isVisible()
    }
}
