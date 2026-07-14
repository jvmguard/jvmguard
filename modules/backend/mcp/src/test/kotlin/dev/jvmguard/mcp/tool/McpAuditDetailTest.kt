package dev.jvmguard.mcp.tool

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class McpAuditDetailTest {

    @Test
    fun smallValuesPassThroughUnchanged() {
        assertNull(McpAuditDetail.cap(null))
        assertEquals(4, McpAuditDetail.cap(4))
        assertEquals("abc", McpAuditDetail.cap("abc"))
        val list = listOf(1, 2, 3)
        assertEquals(list, McpAuditDetail.cap(list))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun oversizedValuesAreReplacedWithABoundedDescriptor() {
        val big = (1..10_000).toList()

        val capped = McpAuditDetail.cap(big) as Map<String, Any?>

        assertEquals(true, capped["truncated"])
        assertTrue((capped["length"] as Int) > 512, "records the real serialized length")
        assertEquals(64, (capped["sha256"] as String).length, "full sha-256 hex digest")
        assertTrue((capped["preview"] as String).length <= 512, "the preview itself is bounded")
    }
}
