package com.jvmguard.ui.e2e.data

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.e2e.PlaywrightE2ETest
import com.jvmguard.ui.views.data.telemetry.TelemetryOverviewPanel
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Runs against a demo-mode server that real agent-instrumented worker JVMs (the demo cluster) connect to.
 * The cluster takes a while to connect, so [waitForDemoVm] polls the VMs grid.
 */
@Tag("data-e2e")
class DataE2ETest : PlaywrightE2ETest() {

    @BeforeAll
    fun resetServer() {
        // The base @BeforeAll created the 'test' admin; reset and recreate it cleanly so the demo VMs
        // can connect against a fresh data directory.
        controlGet("/test?command=reset")
        controlGet("/test?command=createUser")
    }

    @Test
    fun demoClusterReportsRealVmsAndTelemetry() = onPage {
        login()
        waitForDemoVm()

        getByTestId(VmTreeGrid.ID_SHOW).first().click()
        getByTestId(VmTreeGrid.ID_SHOW_TELEMETRIES).click()
        assertThat(getByTestId(TelemetryOverviewPanel.ID_GRID)).isVisible()
    }

    private fun Page.login() {
        navigate("$baseUrl/login", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }

    private fun Page.waitForDemoVm() {
        repeat(POLL_ATTEMPTS) {
            navigate("$baseUrl/", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
            getByTestId(VmTreeGrid.ID_GRID).waitFor()
            if (getByTestId(VmTreeGrid.ID_GRID).getByText("Demo").count() > 0) {
                return
            }
            waitForTimeout(POLL_INTERVAL_MS)
        }
        throw AssertionError("No demo VM connected within ${POLL_ATTEMPTS * POLL_INTERVAL_MS / 1000} s")
    }

    private fun controlGet(path: String) {
        val request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build()
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
            .send(request, HttpResponse.BodyHandlers.discarding())
    }

    private companion object {
        const val POLL_ATTEMPTS = 40
        const val POLL_INTERVAL_MS = 3_000.0
    }
}
