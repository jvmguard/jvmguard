package com.jvmguard.mcp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CaptureRateLimiterTest {

    private val limiter = CaptureRateLimiter()

    @Test
    fun disabledCooldownAlwaysAllows() {
        limiter.recordCapture("Demo/Payment")
        assertNull(limiter.secondsSinceLastWithinCooldown("Demo/Payment", 0))
    }

    @Test
    fun firstCaptureIsAllowed() {
        assertNull(limiter.secondsSinceLastWithinCooldown("Demo/Payment", 60))
    }

    @Test
    fun captureWithinCooldownIsBlocked() {
        limiter.recordCapture("Demo/Payment")
        assertEquals(0L, limiter.secondsSinceLastWithinCooldown("Demo/Payment", 60))
    }

    @Test
    fun cooldownIsPerVm() {
        limiter.recordCapture("Demo/Payment")
        assertNull(limiter.secondsSinceLastWithinCooldown("Demo/Checkout", 60))
    }

    @Test
    fun cooldownKeyIgnoresTrailingSlash() {
        limiter.recordCapture("Demo/Payment")
        assertEquals(0L, limiter.secondsSinceLastWithinCooldown("Demo/Payment/", 60))
    }
}
