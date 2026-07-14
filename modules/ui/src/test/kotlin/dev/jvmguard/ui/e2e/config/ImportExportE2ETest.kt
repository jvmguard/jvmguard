package dev.jvmguard.ui.e2e.config

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import dev.jvmguard.ui.views.settings.ImportExportView
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The import half is not reproduced here: Playwright's `setInputFiles` does not reliably drive the Vaadin
 * `Upload` component's client-side XHR. The import is covered by the browserless `ImportExportViewTest`.
 */
class ImportExportE2ETest : ConfigE2ETest() {

    @Test
    fun exportTriggersDownloadAndImportControlIsPresent() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openGeneralSettings("settings/import-export")
        val download = waitForDownload { getByTestId(ImportExportView.ID_EXPORT).click() }
        assertTrue(download.suggestedFilename().isNotBlank(), "Export should propose a file name.")
        assertThat(getByTestId(ImportExportView.ID_UPLOAD)).isVisible()
    }
}
