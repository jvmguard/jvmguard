package dev.jvmguard.mcp.tool

import dev.jvmguard.agent.config.transactions.TransactionType
import dev.jvmguard.data.transactions.TransactionTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class McpTransactionTreeTest {

    @Test
    fun reportsMicrosAndPerInvocationAverage() {
        val root = TransactionTree(null, TransactionType.DECLARED, null)
        val browse = child(root, "Browse home", count = 100, timeNanos = 10_300_000_000L) // 10.3s total, 103ms avg
        child(browse, "Render page", count = 100, timeNanos = 5_000_000_000L)

        val result = McpTransactionTree.toResult("callTree", root)

        assertEquals("microseconds", result["timeUnit"])
        val nodes = result["callTree"] as List<*>
        val browseNode = nodes.single() as Map<*, *>
        assertEquals("Browse home", browseNode["name"])
        assertEquals(100L, browseNode["count"])
        assertEquals(10_300_000L, browseNode["totalMicros"])
        assertEquals(103_000L, browseNode["avgMicros"])
        assertEquals(5_300_000L, browseNode["selfMicros"]) // total minus the child's 5s
        assertEquals(1, (browseNode["children"] as List<*>).size)
    }

    @Test
    fun hoistsUniformTypeAndOmitsItPerNode() {
        val root = TransactionTree(null, TransactionType.DECLARED, null)
        child(root, "Browse home", count = 1, timeNanos = 1_000_000L)

        val result = McpTransactionTree.toResult("callTree", root)

        assertEquals("declared", result["type"])
        val node = (result["callTree"] as List<*>).single() as Map<*, *>
        assertFalse(node.containsKey("type"), "type is hoisted to the top level when uniform")
    }

    @Test
    fun keepsPerNodeTypeWhenMixed() {
        val root = TransactionTree(null, TransactionType.DECLARED, null)
        child(root, "Declared call", count = 1, timeNanos = 1_000_000L, type = TransactionType.DECLARED)
        child(root, "Matched call", count = 1, timeNanos = 1_000_000L, type = TransactionType.MATCHED)

        val result = McpTransactionTree.toResult("callTree", root)

        assertNull(result["type"], "no top-level type when the tree mixes types")
        val types = (result["callTree"] as List<*>).map { (it as Map<*, *>)["type"] }.toSet()
        assertEquals(setOf("declared", "matched"), types)
    }

    @Test
    fun surfacesErrorPolicy() {
        val root = TransactionTree(null, TransactionType.DECLARED, null)
        child(root, "Failing call", count = 1, timeNanos = 1_000_000L, policy = "NumberFormatException")

        val node = (McpTransactionTree.toResult("callTree", root)["callTree"] as List<*>).single() as Map<*, *>

        assertEquals("error", node["policy"])
        assertEquals("NumberFormatException", node["error"])
    }

    @Test
    fun emptyTreeYieldsEmptyArray() {
        val root = TransactionTree(null, TransactionType.DECLARED, null)

        val result = McpTransactionTree.toResult("overdue", root)

        assertTrue((result["overdue"] as List<*>).isEmpty())
        assertNull(result["type"])
    }

    private fun child(
        parent: TransactionTree,
        name: String,
        count: Long,
        timeNanos: Long,
        type: TransactionType = TransactionType.DECLARED,
        policy: String? = null,
    ): TransactionTree {
        val node = parent.createChild(TransactionTree(name, type, policy))
        node.addCount(count)
        node.addTime(timeNanos)
        return node
    }
}
