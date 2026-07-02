package com.jvmguard.ui.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.LocatorAssertions
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.WaitUntilState
import com.jvmguard.ui.components.recording.triggers.TriggerActionDialog
import com.jvmguard.ui.views.data.VmDataView
import com.jvmguard.ui.views.data.VmSelectorDialog
import com.jvmguard.ui.views.data.telemetry.TelemetryOverviewPanel
import com.jvmguard.ui.views.data.telemetry.VmTelemetryView
import com.jvmguard.ui.views.data.transactions.TransactionsView
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmTreeGrid
import com.jvmguard.ui.views.vms.VmsView
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * Mock data (seed 1234567890): top-level groups Database (5 connected VMs named "DB NN"), Web VMs
 * and ERP; Database is the default-expanded group.
 */
@Tag("e2e")
class VmsViewE2ETest : PlaywrightE2ETest() {

    @Test
    fun vmsView_rendersInsideShell_withGroupsAndSparklines() = onPage {
        openVmsView()

        assertThat(locator("vaadin-app-layout")).isVisible()
        assertThat(getByTestId(VmTreeGrid.ID_GRID)).isVisible()

        for (group in listOf("Database", "Web VMs", "ERP")) {
            assertThat(getByText(group).first()).isVisible()
        }

        assertThat(getByText("DB 01", Page.GetByTextOptions().setExact(true)).first()).isVisible()
        assertThat(locator("jvmguard-sparkline").first()).isVisible()

        screenshot(Page.ScreenshotOptions().setPath(screenshotPath("vms-view.png")))
    }

    @Test
    fun toolbar_rendersAllControls() = onPage {
        openVmsView()

        val controls = listOf(
            VmsView.ID_FILTER_SELECT, VmsView.ID_RANGE_SELECT, VmsView.ID_SCALE_SELECT, VmsView.ID_TELEMETRIES
        )
        for (control in controls) {
            assertThat(getByTestId(control)).isVisible()
        }
    }

    @Test
    fun actionsMenu_recordsCpuDataViaSharedDialog() = onPage {
        openVmsView()

        getByTestId(VmTreeGrid.ID_ACTIONS).first().click()
        getByTestId(VmTreeGrid.ID_ACTION_RECORD_JPS).click()
        assertThat(getByTestId(TriggerActionDialog.ID_SAVE)).isVisible()
        getByTestId(TriggerActionDialog.ID_SAVE).click()
        assertThat(getByTestId(TriggerActionDialog.ID_SAVE)).isHidden()
    }

    @Test
    fun showMenu_opensTransactions() = onPage {
        openVmsView()

        getByTestId(VmTreeGrid.ID_SHOW).first().click()
        getByTestId(VmTreeGrid.ID_SHOW_TRANSACTIONS).click()

        assertThat(this).hasURL(Pattern.compile(".*/transactions$"))
        assertThat(getByTestId(TransactionsView.ID_GRID)).isVisible()
        assertThat(locator(".jvmguard-breadcrumb-current")).hasText("Database")
    }

    @Test
    fun showMenu_telemetries_opensTheOverview() = onPage {
        openVmsView()

        getByTestId(VmTreeGrid.ID_SHOW).first().click()
        getByTestId(VmTreeGrid.ID_SHOW_TELEMETRIES).click()

        assertThat(this).hasURL(Pattern.compile(".*/telemetry.*"))
        assertThat(getByTestId(VmTelemetryView.ID_MODE)).isVisible()
        assertThat(getByTestId(TelemetryOverviewPanel.ID_GRID)).isVisible()
        assertFalse(url().contains("vm="), "the VM selection must not appear in the URL")
    }

    @Test
    fun showMenu_omitsMBeansForPlainGroups() = onPage {
        openVmsView()

        getByTestId(VmTreeGrid.ID_SHOW).first().click()
        assertThat(getByTestId(VmTreeGrid.ID_SHOW_TRANSACTIONS)).isVisible()
        assertThat(getByTestId(VmTreeGrid.ID_SHOW_MBEANS)).hasCount(0)
    }

    @Test
    fun showMenu_vm_opensMBeans() = onPage {
        openVmsView()

        val db01Show = locator("vaadin-grid-cell-content")
            .filter(Locator.FilterOptions().setHasText("DB 01"))
            .getByTestId(VmTreeGrid.ID_SHOW).first()
        db01Show.click()
        getByTestId(VmTreeGrid.ID_SHOW_MBEANS).click()

        assertThat(this).hasURL(Pattern.compile(".*/mbeans$"))
        assertThat(locator(".jvmguard-breadcrumb-current")).hasText("DB 01")
    }

    @Test
    fun clickingSparkline_navigatesToTelemetryView() = onPage {
        openVmsView()

        locator("jvmguard-sparkline").first().click()

        assertThat(this).hasURL(Pattern.compile(".*/telemetry\\?.*t=.*"))
        assertFalse(url().contains("vm="), "the VM selection must not appear in the URL")
        assertThat(getByTestId(VmDataView.ID_SELECT_BUTTON)).isVisible()
        assertThat(locator(".jvmguard-breadcrumb-current")).hasText("Database")
    }

    @Test
    fun telemetrySelectButtonOpensTheVmSelectorDialog() = onPage {
        openVmsView()
        locator("jvmguard-sparkline").first().click()

        getByTestId(VmDataView.ID_SELECT_BUTTON).click()

        assertThat(locator("vaadin-dialog-overlay")).isVisible()
        assertThat(getByText("All JVMs").first()).isVisible()
        assertThat(getByTestId(VmSelectorDialog.ID_SELECT)).isVisible()
    }

    @Test
    fun selectorDialogSelectsAGroupByClick() = onPage {
        openVmsView()
        locator("jvmguard-sparkline").first().click()
        getByTestId(VmDataView.ID_SELECT_BUTTON).click()

        // Regression guard: clicking a parent (group) row must select it, not merely toggle it (the
        // vaadin-grid-tree-toggle swallowing the click).
        getByText("ERP", Page.GetByTextOptions().setExact(true)).first().click()
        getByTestId(VmSelectorDialog.ID_SELECT).click()

        assertThat(locator(".jvmguard-breadcrumb-current")).hasText("ERP")
        assertThat(this).hasURL(Pattern.compile(".*/telemetry.*"))
        assertFalse(url().contains("vm="), "selecting a VM must not put it in the URL")
    }

    @Test
    fun keyboard_activatesShowMenu_withEnter() = onPage {
        openVmsView()

        getByTestId(VmTreeGrid.ID_SHOW).first().press("Enter")
        getByTestId(VmTreeGrid.ID_SHOW_TRANSACTIONS).click()
        assertThat(getByTestId(TransactionsView.ID_GRID)).isVisible()
    }

    @Test
    fun actionsMenu_runGc_confirmsImmediately() = onPage {
        openVmsView()

        getByTestId(VmTreeGrid.ID_ACTIONS).first().click()
        getByTestId(VmTreeGrid.ID_ACTION_GC).click()

        // The mock's runGC sleeps 5s; the tight timeout guards against a regression to a blocking
        // (on-UI-thread) call by requiring the confirmation immediately.
        assertThat(locator("vaadin-notification-card").first())
            .isVisible(LocatorAssertions.IsVisibleOptions().setTimeout(3000.0))
    }

    @Test
    fun navigationDrawer_switchesBetweenViews() = onPage {
        openVmsView()

        assertThat(locator("vaadin-drawer-toggle")).isHidden()
        val nav = locator("vaadin-side-nav")
        nav.getByText("Transactions").click()
        assertThat(this).hasURL(Pattern.compile(".*/transactions.*"))
        assertThat(getByTestId(TransactionsView.ID_GRID)).isVisible()

        nav.getByText("Telemetries").click()
        assertThat(this).hasURL(Pattern.compile(".*/telemetry.*"))
        assertThat(getByTestId(VmTelemetryView.ID_MODE)).isVisible()
        assertThat(getByTestId(VmTelemetryView.ID_CHART)).isVisible()

        nav.getByText("VMs").click()
        assertThat(getByTestId(VmTreeGrid.ID_GRID)).isVisible()
    }

    @Test
    fun selectionRidesTheModelAcrossViews_withBareUrls() = onPage {
        openVmsView()

        locator("vaadin-grid-cell-content")
            .filter(Locator.FilterOptions().setHasText("DB 01"))
            .getByTestId(VmTreeGrid.ID_SHOW).first().click()
        getByTestId(VmTreeGrid.ID_SHOW_TRANSACTIONS).click()
        assertThat(locator(".jvmguard-breadcrumb-current")).hasText("DB 01")

        val nav = locator("vaadin-side-nav")
        nav.getByText("Telemetries").click()
        assertThat(this).hasURL(Pattern.compile(".*/telemetry$"))
        assertThat(locator(".jvmguard-breadcrumb-current")).hasText("DB 01")

        nav.getByText("MBeans").click()
        assertThat(this).hasURL(Pattern.compile(".*/mbeans$"))
        assertThat(locator(".jvmguard-breadcrumb-current")).hasText("DB 01")

        // Passing through the selection-less VMs view must not drop the selection.
        nav.getByText("VMs").click()
        assertThat(this).hasURL(Pattern.compile(".*/?$"))
        nav.getByText("Transactions").click()
        assertThat(this).hasURL(Pattern.compile(".*/transactions$"))
        assertThat(locator(".jvmguard-breadcrumb-current")).hasText("DB 01")
    }

    @Test
    fun navigationDrawer_firstItemAlignsWithContentCard() = onPage {
        openVmsView()
        assertThat(locator("vaadin-side-nav-item").first()).isVisible()

        @Suppress("UNCHECKED_CAST")
        val tops = evaluate(
            """
            () => {
              const a = document.querySelector('vaadin-app-layout');
              const card = [...a.children].find(c => !c.getAttribute('slot'));
              const sni = document.querySelector('vaadin-side-nav-item');
              const part = sni.shadowRoot.querySelector('[part~="item"]') || sni;
              return { card: Math.round(card.getBoundingClientRect().top),
                       item: Math.round(part.getBoundingClientRect().top) };
            }
            """.trimIndent()
        ) as Map<String, Any>
        val cardTop = (tops.getValue("card") as Number).toInt()
        val itemTop = (tops.getValue("item") as Number).toInt()
        assertTrue(abs(cardTop - itemTop) <= 6) {
            "first nav item should align with the content card top; cardTop=$cardTop itemTop=$itemTop"
        }
    }

    @Test
    fun contentCard_hasSymmetricTopSpacing() = onPage {
        openVmsView()

        // Symmetry is checked against the right inset (viewport edge): the left side is bounded by the
        // open navigation drawer, which the drawer does not affect.
        @Suppress("UNCHECKED_CAST")
        val gaps = evaluate(
            """
            () => {
              const a = document.querySelector('vaadin-app-layout');
              const card = [...a.children].find(c => !c.getAttribute('slot'));
              const nav = a.shadowRoot.querySelector('[part~=navbar]');
              const r = card.getBoundingClientRect();
              const nr = nav.getBoundingClientRect();
              return { top: Math.round(r.top - nr.bottom), right: Math.round(window.innerWidth - r.right) };
            }
            """.trimIndent()
        ) as Map<String, Any>
        val topGap = (gaps.getValue("top") as Number).toInt()
        val rightGap = (gaps.getValue("right") as Number).toInt()
        assertTrue(topGap > 0) { "content card should not abut the header; topGap=$topGap" }
        assertTrue(abs(topGap - rightGap) <= 8) {
            "top/side spacing should be symmetric; topGap=$topGap rightGap=$rightGap"
        }
    }

    @Test
    fun gridCells_useTighterPaddingThanAuraDefault() = onPage {
        openVmsView()

        @Suppress("UNCHECKED_CAST")
        val pad = evaluate(
            """
            () => {
              const grid = document.querySelector('vaadin-grid.jvmguard-vm-grid');
              const content = grid.querySelector('vaadin-grid-cell-content');
              const cs = getComputedStyle(content);
              return { contentLeft: parseFloat(cs.paddingLeft), contentTop: parseFloat(cs.paddingTop) };
            }
            """.trimIndent()
        ) as Map<String, Any>
        val contentLeft = (pad.getValue("contentLeft") as Number).toDouble()
        assertTrue(contentLeft < 8) {
            "VMs grid columns should be tighter than the Aura default (~8.8px) → $pad"
        }
    }

    private fun Page.openVmsView() {
        // Not NETWORKIDLE: @Push keeps a websocket open, so the network never goes idle.
        navigate("$baseUrl/login?mock", Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD))
        // The test id is on the <vaadin-text-field> host; fill() needs the inner native <input>.
        getByTestId(LoginView.ID_USERNAME).locator("input").fill("test")
        getByTestId(LoginView.ID_PASSWORD).locator("input").fill("password4329")
        getByTestId(LoginView.ID_SUBMIT).click()

        getByTestId(VmTreeGrid.ID_GRID).waitFor()
        assertThat(getByText("Database").first()).isVisible()
    }

}
