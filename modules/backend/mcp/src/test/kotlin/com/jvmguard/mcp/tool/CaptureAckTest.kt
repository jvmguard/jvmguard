package com.jvmguard.mcp.tool

import com.jvmguard.data.file.SnapshotFileType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CaptureAckTest {

    @Test
    fun recordingAckCarriesReadinessAndPollingRecipe() {
        val ack = captureAck(
            status = "recording",
            vmPath = "Demo/Purchase/Payment",
            type = SnapshotFileType.JFR,
            triggeredAt = 1_000_000L,
            estimatedSeconds = 25,
            durationSeconds = 20,
        )

        assertEquals("recording", ack["status"])
        assertEquals("JFR", ack["type"])
        assertEquals(20, ack["durationSeconds"])
        assertEquals(1_000_000L, ack["triggeredAt"])
        assertEquals(25, ack["estimatedSeconds"])
        assertEquals(1_025_000L, ack["estimatedReadyAt"]) // triggeredAt + estimatedSeconds * 1000
        val nextStep = ack["nextStep"] as String
        assertTrue(nextStep.contains("type=\"JFR\""))
        assertTrue(nextStep.contains("since=1000000"))
        assertTrue(nextStep.contains("vm=\"Demo/Purchase/Payment\""))
    }

    @Test
    fun echoesExtrasSuchAsSubsystems() {
        val ack = captureAck(
            status = "recording",
            vmPath = "Demo/Purchase/Payment",
            type = SnapshotFileType.JPS,
            triggeredAt = 1L,
            estimatedSeconds = 25,
            durationSeconds = 20,
            extras = mapOf("subsystems" to listOf("cpu", "socket")),
        )

        assertEquals(listOf("cpu", "socket"), ack["subsystems"])
    }

    @Test
    fun instantCaptureOmitsDuration() {
        val ack = captureAck(
            status = "capturing",
            vmPath = "Demo/Purchase/Payment",
            type = SnapshotFileType.THREAD_DUMP,
            triggeredAt = 5L,
            estimatedSeconds = 2,
        )

        assertFalse(ack.containsKey("durationSeconds"))
        assertEquals(2005L, ack["estimatedReadyAt"])
    }
}
