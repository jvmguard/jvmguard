package com.jvmguard.ui.e2e.screenshots

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import com.jvmguard.ui.views.data.telemetry.TelemetryNavigationBar
import com.jvmguard.ui.views.data.telemetry.TelemetryOverviewPanel
import com.jvmguard.ui.views.data.telemetry.VmTelemetryView
import com.jvmguard.ui.views.vms.VmTreeGrid
import com.jvmguard.ui.views.vms.VmsView
import org.junit.jupiter.api.Test

class TelemetryScreenshots : ScreenshotTest() {

    @Test
    fun vmsSparklines() = onPage {
        login(demo = true)
        getByTestId(VmTreeGrid.ID_SHOW).first().click()
        getByTestId(VmTreeGrid.ID_SHOW_TELEMETRIES).click()
        getByTestId(TelemetryOverviewPanel.ID_GRID).waitFor()
        capture("vms_sparklines")
    }

    @Test
    fun telemetryTransactions() = onPage {
        login(demo = true)
        open("telemetry?${vmQuery(DEMO_VM)}")
        getByTestId(VmTelemetryView.ID_CHART).waitFor()
        selectTelemetryType("Transactions", expectSeries = "Normal")
        capture("telemetry_transactions")
    }

    @Test
    fun telemetryHeap() = onPage {
        login(demo = true)
        open("telemetry?${vmQuery(DEMO_VM)}")
        getByTestId(VmTelemetryView.ID_CHART).waitFor()
        selectTelemetryType("Heap Usage", expectSeries = "Used")
        capture("telemetry_heap")
    }

    @Test
    fun sparklinesConfigure() = onPage {
        login(demo = true)
        // In V2 sparkline columns come from the "Telemetries" multi-select combo box, not a separate dialog.
        getByTestId(VmsView.ID_TELEMETRIES).click()
        assertThat(locator("vaadin-multi-select-combo-box-overlay")).isVisible()
        capture("sparklines_configure")
    }

    @Test
    fun telemetryTransactions3Hours() = onPage {
        login(demo = true)
        open("telemetry?${vmQuery(DEMO_VM)}")
        getByTestId(VmTelemetryView.ID_CHART).waitFor()
        selectTelemetryType("Transactions", expectSeries = "Normal")
        getByTestId(TelemetryNavigationBar.ID_INTERVAL).click()
        selectOverlayOption("3 hours")
        // Wait until the chart has actually re-rendered at the 3-hour extent (see echart.ts data-xextent).
        locator("jvmguard-echart[data-xextent=\"${3 * 60 * 60 * 1000}\"]").first().waitFor()
        capture("telemetry_transactions_3hours")
    }

    /**
     * Picks a telemetry type from the [VmTelemetryView] type [com.vaadin.flow.component.select.Select] and waits until
     * the chart has actually re-rendered with that metric. Scoping the click to `vaadin-select-overlay` (and confirming
     * via the chart's `data-series`, set in echart.ts) is what makes the selection reliably take effect, rather than
     * capturing the previously shown metric.
     */
    private fun Page.selectTelemetryType(label: String, expectSeries: String) {
        getByTestId(VmTelemetryView.ID_TYPE_SELECT).click()
        selectOverlayOption(label)
        locator("jvmguard-echart[data-series*=\"$expectSeries\"]").first().waitFor()
    }

    /**
     * Clicks an item in an open Vaadin `Select` overlay. The list-box is slotted into the overlay, so its DOM parent is
     * the `<vaadin-select>`, not the overlay element; locating by the option role (accessible name) is what reliably
     * reaches the item and actually fires the selection.
     */
    private fun Page.selectOverlayOption(label: String) {
        getByRole(AriaRole.OPTION, Page.GetByRoleOptions().setName(label).setExact(true)).click()
    }
}
