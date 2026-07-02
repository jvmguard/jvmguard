package com.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("e2e")
class DropdownMenuPlacementE2ETest : PlaywrightE2ETest() {

    @Test
    fun dropdownMenuOpensBelowItsButton() = onPage {
        login()
        navigate("$baseUrl/recording/triggers?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))

        val button = getByTestId("trigger-add")
        for (attempt in 1..3) {
            // Click the right edge: a left-aligned menu proves the anchor is the button, not the cursor.
            button.click(com.microsoft.playwright.Locator.ClickOptions().setPosition(button.boundingBox().width - 4.0, 4.0))

            val overlay = locator("vaadin-menu-bar-overlay[opened]")
            overlay.waitFor()
            val buttonBox = button.boundingBox()
            val menuBox = overlay.boundingBox()
            if (attempt == 3) {
                screenshot(Page.ScreenshotOptions().setPath(screenshotPath("dropdown-menu-placement.png")))
            }

            assertTrue(
                menuBox.y >= buttonBox.y + buttonBox.height - 2.0,
                "open #$attempt: menu top ${menuBox.y} should be at/below the button bottom ${buttonBox.y + buttonBox.height}",
            )
            assertTrue(
                kotlin.math.abs(menuBox.x - buttonBox.x) <= 6.0,
                "open #$attempt: menu left ${menuBox.x} should align with the button left ${buttonBox.x}, not the click point",
            )

            keyboard().press("Escape")
            overlay.waitFor(com.microsoft.playwright.Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED))
        }
    }

    private fun Page.login() {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()
    }
}
