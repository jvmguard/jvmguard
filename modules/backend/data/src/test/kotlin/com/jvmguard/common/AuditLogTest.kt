package com.jvmguard.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuditLogTest {

    @Test
    fun buildsOrderedEventWithAllFields() {
        val event = AuditLog.buildEvent(
            source = "mcp",
            principal = "alice",
            action = "record_jps",
            outcome = AuditLog.Outcome.OK,
            target = "Demo/Purchase/Payment",
            detail = "subsystems=[cpu, socket]",
            clientIp = "10.0.0.5",
        )

        assertEquals(
            listOf("source", "principal", "clientIp", "action", "target", "outcome", "detail"),
            event.keys.toList(),
        )
        assertEquals("alice", event["principal"])
        assertEquals("10.0.0.5", event["clientIp"])
        assertEquals("ok", event["outcome"])
    }

    @Test
    fun unauthenticatedPrincipalIsExplicitNullAndOptionalFieldsOmitted() {
        val event = AuditLog.buildEvent(
            source = "mcp",
            principal = null,
            action = "auth",
            outcome = AuditLog.Outcome.AUTH_FAILED,
            target = null,
            detail = null,
            clientIp = null,
        )

        assertTrue(event.containsKey("principal"))
        assertEquals(null, event["principal"])
        assertFalse(event.containsKey("clientIp"))
        assertFalse(event.containsKey("target"))
        assertFalse(event.containsKey("detail"))
        assertEquals("auth_failed", event["outcome"])
    }

    @Test
    fun onlyFailuresAndDenialsAreWarnLevel() {
        assertFalse(AuditLog.Outcome.OK.warn)
        assertTrue(AuditLog.Outcome.ERROR.warn)
        assertTrue(AuditLog.Outcome.DENIED.warn)
        assertTrue(AuditLog.Outcome.AUTH_FAILED.warn)
    }
}
