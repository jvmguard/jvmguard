package com.jvmguard.ui.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Base class for the real-browser (Playwright) E2E tests. The suite auto-skips if no jvmguard server
 * (with the integration-test control filter) is reachable at `-Pjvmguard.e2e.url` (default
 * `http://localhost:8020`); the `test`/`password4329` user is created (as ADMIN) via
 * `IntegrationTestControlController` (`/test?command=createUser`).
 *
 * `PER_CLASS` lets [ensureServerAndBrowser] be a plain (non-static) `@BeforeAll`.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class PlaywrightE2ETest {

    protected val baseUrl: String = System.getProperty("jvmguard.e2e.url", "http://localhost:8020")
    private val screenshotDir: String = System.getProperty("jvmguard.e2e.screenshotDir", "build/e2e")

    /** When set, pages render in the dark color scheme (the app honors `prefers-color-scheme`, see AppShell). */
    protected val darkScreenshots: Boolean = System.getProperty("jvmguard.e2e.darkScreenshots") == "true"

    /**
     * Default per-action timeout. The fast e2e suite keeps the short default; the screenshot tasks raise it (via
     * `jvmguard.e2e.timeoutMs`) because dev mode compiles each route on its first hit, which can exceed 15s.
     */
    private val defaultTimeoutMs: Double =
        System.getProperty("jvmguard.e2e.timeoutMs")?.toDoubleOrNull() ?: SHORT_TIMEOUT_MS

    // Follow redirects: Spring MVC may answer /test with a trailing-slash 301.
    private val http: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL).build()

    private lateinit var playwright: Playwright
    private lateinit var browser: Browser

    @BeforeAll
    @Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun ensureServerAndBrowser() {
        assumeTrue(
            status("/login") == 200,
            "No jvmguard UI at $baseUrl — start ServerMain (or set -Pjvmguard.e2e.url). Skipping E2E."
        )
        assumeTrue(
            status("/test?command=ping") < 400,
            "Integration-test control filter not enabled (jvmguard.testControlFilter). Skipping E2E."
        )
        status("/test?command=createUser")

        playwright = Playwright.create()
        browser = playwright.chromium().launch(BrowserType.LaunchOptions().setTimeout(LAUNCH_TIMEOUT_MS))
    }

    @AfterAll
    fun closeBrowser() {
        runCatching { if (::browser.isInitialized) browser.close() }
        runCatching { if (::playwright.isInitialized) playwright.close() }
    }

    /**
     * Runs [block] against a fresh 1400x900 browser page (as its receiver). [deviceScaleFactor] > 1
     * simulates a HiDPI display, where sub-pixel layout rounding surfaces bugs a 1:1 viewport hides.
     */
    protected fun onPage(deviceScaleFactor: Double = 1.0, block: Page.() -> Unit) {
        val contextOptions = Browser.NewContextOptions().setViewportSize(1400, 900).setDeviceScaleFactor(deviceScaleFactor)
        if (darkScreenshots) {
            contextOptions.setColorScheme(com.microsoft.playwright.options.ColorScheme.DARK)
        }
        browser.newContext(contextOptions).use { context ->
            val page = context.newPage()
            page.setDefaultTimeout(defaultTimeoutMs)
            page.setDefaultNavigationTimeout(maxOf(defaultTimeoutMs, NAV_TIMEOUT_MS))
            val jsErrors = Collections.synchronizedList(mutableListOf<String>())
            page.onPageError { jsErrors.add(it) }
            try {
                page.block()
            } catch (e: Throwable) {
                if (jsErrors.isNotEmpty()) {
                    throw AssertionError("Client-side JS error(s) during the test: $jsErrors", e)
                }
                throw e
            }
            check(jsErrors.isEmpty()) { "Client-side JS error(s) during the test: $jsErrors" }
        }
    }

    protected fun screenshotPath(fileName: String): Path = Paths.get(screenshotDir, fileName)

    /** Issues an `IntegrationTestControlController` command (e.g. `reset`, `createUser`); returns the HTTP status. */
    protected fun controlCommand(command: String): Int = status("/test?command=$command")

    /** GETs [path] with optional HTTP Basic credentials (the REST API's `user`/`apiKey`); returns the status. */
    protected fun apiStatus(path: String, user: String? = null, apiKey: String? = null): Int = runCatching {
        // REST endpoints produce only text/plain and content negotiation defaults to application/json,
        // so without this Accept the request 404s via the /api/** fallback before security runs.
        val builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(5))
            .header("Accept", "text/plain").GET()
        if (user != null) {
            val basic = Base64.getEncoder().encodeToString("$user:$apiKey".toByteArray())
            builder.header("Authorization", "Basic $basic")
        }
        http.send(builder.build(), HttpResponse.BodyHandlers.discarding()).statusCode()
    }.getOrDefault(-1)

    private companion object {
        const val SHORT_TIMEOUT_MS = 15_000.0
        const val NAV_TIMEOUT_MS = 20_000.0
        const val LAUNCH_TIMEOUT_MS = 30_000.0
    }

    private fun status(path: String): Int = runCatching {
        val request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(5)).GET().build()
        http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
    }.getOrDefault(-1)
}
