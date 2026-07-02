package com.jvmguard.ui.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.views.data.VmDataView
import com.jvmguard.ui.views.data.mbeans.AttributeEditDialog
import com.jvmguard.ui.views.data.mbeans.MBeansView
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmTreeGrid
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("e2e")
class MBeansViewE2ETest : PlaywrightE2ETest() {

    @Test
    fun rendersTheMBeanTreeForASingleVm() = onPage {
        openMBeans("Database/DB 01")

        // Also proves the reused folded com.jvmguard.ui.views.data.mbeans helpers load in the ee11 web context.
        assertThat(getByTestId(MBeansView.ID_TREE)).isVisible()
        assertThat(getByTestId(VmDataView.ID_SELECT_BUTTON)).isVisible()
        assertThat(getByText("java.lang").first()).isVisible()

        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("mbeans.png")))
    }

    @Test
    fun groupSelection_promptsForASingleVm() = onPage {
        openMBeans("")

        assertThat(getByTestId(MBeansView.ID_SELECT_VM)).isVisible()
        assertThat(getByTestId(MBeansView.ID_TREE)).hasCount(0)
    }

    @Test
    fun editingAWritableAttribute_updatesItsValue() = onPage {
        selectMemoryBean()

        // Memory.Verbose is a writable boolean, initially off: a real round-trip through
        // setMBeanAttribute against the server JVM's own platform MBean.
        assertThat(getByTestId(MBeansView.ID_VALUE_PREFIX + "Verbose")).hasText("false")
        getByTestId(MBeansView.ID_EDIT_PREFIX + "Verbose").click()
        getByRole(AriaRole.CHECKBOX).check()
        getByTestId(AttributeEditDialog.ID_SAVE).click()

        assertThat(getByTestId(MBeansView.ID_VALUE_PREFIX + "Verbose")).hasText("true")
    }

    @Test
    fun invokingAVoidOperation_showsSuccess() = onPage {
        selectMemoryBean()

        // gc() takes no arguments and returns void, so invoking it skips the parameter dialog.
        getByText("Operations").click()
        getByTestId(MBeansView.ID_INVOKE_PREFIX + "gc").click()

        assertThat(locator("vaadin-notification-card")).isVisible()
    }

    @Test
    fun keepAlive_retainsSelectionFilterAndTabAcrossViewSwitch() = onPage {
        selectMemoryBean()
        getByText("Operations").click()
        assertThat(getByTestId(MBeansView.ID_INVOKE_PREFIX + "gc")).isVisible()

        val nav = locator("vaadin-side-nav")
        nav.getByText("VMs").click()
        assertThat(getByTestId(VmTreeGrid.ID_GRID)).isVisible()
        nav.getByText("MBeans").click()

        // The kept-alive instance is re-attached, not rebuilt: filter text, selected bean and active tab survive.
        assertThat(getByTestId(MBeansView.ID_FILTER).locator("input")).hasValue("java.lang:type=Memory")
        assertThat(getByTestId(MBeansView.ID_INVOKE_PREFIX + "gc")).isVisible()
    }

    private fun Page.selectMemoryBean() {
        openMBeans("Database/DB 01")
        getByTestId(MBeansView.ID_TREE).waitFor()
        getByTestId(MBeansView.ID_FILTER).locator("input").fill("java.lang:type=Memory")
        getByText("Memory [type]").click()
        assertThat(getByTestId(MBeansView.ID_VALUE_PREFIX + "Verbose")).isVisible()
    }

    private fun Page.openMBeans(vmPath: String) {
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()
        getByTestId(VmTreeGrid.ID_GRID).waitFor()

        val encoded = vmPath.replace(" ", "%20")
        navigate("$baseUrl/mbeans?vm=$encoded", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
    }
}
