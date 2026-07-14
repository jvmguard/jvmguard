package dev.jvmguard.ui.components.sparkline

import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SparklineStateTest {

    @Test
    fun shortValuePassesThroughUnchanged() {
        assertEquals("42", SparklineState.format(42, TelemetryUnit.PERCENT))
    }

    @Test
    fun fractionalValueIsRoundedWhenItFitsTheDisplayDigits() {
        // "12.7" exceeds PERCENT's 3 display digits, so it rounds to "13".
        assertEquals("13", SparklineState.format(12.7, TelemetryUnit.PERCENT))
    }

    @Test
    fun valueTooLongEvenRoundedFallsBackToScientificNotation() {
        // 12345.6 rounds to "12346" (5 chars > 3 digits), falling back to "1E4".
        assertEquals("1E4", SparklineState.format(12345.6, TelemetryUnit.PERCENT))
    }

    @Test
    fun bigDecimalIsRenderedPlain() {
        assertEquals("7", SparklineState.format(BigDecimal("7"), TelemetryUnit.PLAIN))
    }

    @Test
    fun valueAboveIntRangeFormatsWithoutOverflow() {
        // Value > Int.MAX must round with Long semantics, not clamp; BYTES has 4 labels so the
        // single-label K/M/G branch is skipped, and it falls back to scientific notation.
        assertEquals("3E9", SparklineState.format(3_000_000_000L, TelemetryUnit.BYTES))
    }

    @Test
    fun emptyStateHasSafeDefaults() {
        val state = SparklineState.empty()
        assertEquals(0, state.data.size)
        assertEquals(1.0, state.displayRangeMax)
        assertEquals(0.0, state.displayRangeMin)
    }
}
