package dev.jvmguard.ui.e2e.screenshots

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import dev.jvmguard.ui.views.data.mbeans.MBeansView
import dev.jvmguard.ui.views.data.mbeans.ParameterDialog
import org.junit.jupiter.api.Test

class MBeansScreenshots : ScreenshotTest() {

    @Test
    fun mbeanOperations() = onPage {
        login()
        open("mbeans?${vmQuery("Database/DB 01")}")
        getByTestId(MBeansView.ID_TREE).waitFor()
        getByTestId(MBeansView.ID_FILTER).locator("input").fill("java.lang:type=Memory")
        getByText("Memory [type]").click()
        getByText("Operations").click()
        assertThat(getByTestId(MBeansView.ID_INVOKE_PREFIX + "gc")).isVisible()
        capture("mbean_operations")
    }

    @Test
    fun mbeanOperationArguments() = onPage {
        openLoggingOperations()
        getByTestId(MBeansView.ID_INVOKE_PREFIX + OPERATION).click()
        assertThat(getByTestId(ParameterDialog.ID_INVOKE)).isVisible()
        capture("mbean_operation_arguments")
    }

    @Test
    fun mbeanOperationReturn() = onPage {
        openLoggingOperations()
        getByTestId(MBeansView.ID_INVOKE_PREFIX + OPERATION).click()
        assertThat(getByTestId(ParameterDialog.ID_INVOKE)).isVisible()
        // The parameter is labelled by its type and position.
        getByLabel("java.lang.String p0").fill("global")
        getByTestId(ParameterDialog.ID_INVOKE).click()
        // OperationResultDialog has no test id; located by its header text.
        assertThat(getByText("Operation result")).isVisible()
        capture("mbean_operation_return")
    }

    // getLoggerLevel(String) is non-overloaded, takes one String and returns one, so it drives both the parameter and result dialogs.
    private fun Page.openLoggingOperations() {
        login()
        open("mbeans?${vmQuery("Database/DB 01")}")
        getByTestId(MBeansView.ID_TREE).waitFor()
        getByTestId(MBeansView.ID_FILTER).locator("input").fill("java.util.logging:type=Logging")
        getByText("Logging [type]").click()
        getByText("Operations").click()
        assertThat(getByTestId(MBeansView.ID_INVOKE_PREFIX + OPERATION)).isVisible()
    }

    companion object {
        private const val OPERATION = "getLoggerLevel"
    }
}
