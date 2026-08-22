package dev.jvmguard.mcp.tool

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RedactGuardrailTest {

    @Test
    fun withoutGuardrailTheRequestDecides() {
        assertFalse(effectiveRedact(false, null))
        assertFalse(effectiveRedact(false, false))
        assertTrue(effectiveRedact(false, true))
    }

    @Test
    fun guardrailForcesRedactionOn() {
        assertTrue(effectiveRedact(true, null))
        assertTrue(effectiveRedact(true, false))
        assertTrue(effectiveRedact(true, true))
    }

    @Test
    fun noteOnlyWhenTheGuardrailOverridesTheRequest() {
        assertTrue(redactForcedNote(false, null).isEmpty())
        assertTrue(redactForcedNote(false, false).isEmpty())
        assertTrue(redactForcedNote(true, true).isEmpty())
        assertFalse(redactForcedNote(true, null).isEmpty())
        assertFalse(redactForcedNote(true, false).isEmpty())
    }
}
