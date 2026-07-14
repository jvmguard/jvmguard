package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TelemetryNavigationBarTest : JvmGuardBrowserlessTest() {

    private val now = 1_700_000_000_000L
    private var changes = 0

    @BeforeEach
    fun setUp() {
        changes = 0
    }

    @Test
    fun defaultsToTenMinutesEndingNow() {
        val bar = bar()
        assertEquals(TelemetryInterval.TEN_MINUTES, bar.selectedInterval)
        assertTrue(bar.isNowSelected())
        assertEquals(now, bar.currentEndTime())
    }

    @Test
    fun zoomOutWidensAndZoomInNarrowsTheInterval() {
        val bar = bar()

        bar.zoomOut(null)
        assertEquals(TelemetryInterval.TWENTY_MINUTES, bar.selectedInterval)
        assertTrue(changes > 0)

        bar.zoomIn(null)
        assertEquals(TelemetryInterval.TEN_MINUTES, bar.selectedInterval)
    }

    @Test
    fun zoomInAtTheNarrowestIntervalIsANoOp() {
        val bar = bar()
        val before = changes

        bar.zoomIn(null)

        assertEquals(TelemetryInterval.TEN_MINUTES, bar.selectedInterval)
        assertEquals(before, changes)
    }

    @Test
    fun zoomingIntoAPastTimeSwitchesToTheSelectedDate() {
        val bar = bar()

        bar.zoomOut(now - 10_000_000L)

        assertFalse(bar.isNowSelected())
        assertTrue(bar.currentEndTime() < now)
    }

    private fun bar() = TelemetryNavigationBar(serverTime = { now }, onChange = { changes++ })
}
