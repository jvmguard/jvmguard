package dev.jvmguard.ui.e2e

import dev.jvmguard.ui.shell.LanguageSelect
import dev.jvmguard.ui.views.login.LoginView
import dev.jvmguard.ui.views.vms.VmTreeGrid
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class LocalizationE2ETest : PlaywrightE2ETest() {

    @Test
    @Order(1)
    fun languageSelectorSwitchesLocaleAndResetsToAuto() = onPage {
        login()
        assertThat(getByText("Transactions").first()).isVisible()
        assertThat(getByTestId(LanguageSelect.ID)).isVisible()

        getByTestId(LanguageSelect.ID).click()
        getByTestId("${LanguageSelect.ID}-ja").click()
        getByText("トランザクション").first().waitFor()

        // Back to Auto: the choice is persisted per user and must not leak into other tests
        getByTestId(LanguageSelect.ID).click()
        getByTestId(LanguageSelect.ID_AUTO).click()
        getByText("Transactions").first().waitFor()
    }

    @Test
    @Order(2)
    fun autoDetectsKorean() = assertAutoDetected("ko", "로그인", "트랜잭션")

    @Test
    @Order(3)
    fun autoDetectsJapanese() = assertAutoDetected("ja", "ログイン", "トランザクション")

    @Test
    @Order(4)
    fun autoDetectsSimplifiedChinese() = assertAutoDetected("zh-CN", "登录", "事务")

    private fun assertAutoDetected(locale: String, loginLabel: String, navLabel: String) = onPage(locale = locale) {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        // The login screen has no selector by design; Accept-Language localizes it
        assertThat(getByTestId(LoginView.ID_SUBMIT)).hasText(loginLabel)
        login()
        assertThat(getByText(navLabel).first()).isVisible()
    }

    private fun Page.login() {
        if (!url().contains("/login")) {
            navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        }
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }
}
