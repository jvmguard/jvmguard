package com.jvmguard.ui.views.data.transactions

import com.jvmguard.data.transactions.TransactionTreeInterval
import tools.jackson.databind.json.JsonMapper

object TransactionExport {

    private val mapper = JsonMapper.builder().build()

    fun toJson(
        mode: TransactionMode,
        roots: List<TransactionNode>,
        interval: TransactionTreeInterval,
        endTime: Long,
    ): ByteArray {
        val root = linkedMapOf<String, Any?>(
            "view" to mode.label,
            "interval" to interval.toString(),
            "endTime" to endTime,
            "transactions" to roots.map(::node),
        )
        return mapper.writeValueAsBytes(root)
    }

    private fun node(node: TransactionNode): Map<String, Any?> =
        linkedMapOf<String, Any?>().apply {
            put("name", node.name)
            node.transactionType?.let { put("type", it.name) }
            node.policyPrefix?.let { put("policy", it) }
            put("totalTimeNanos", node.time)
            put("invocations", node.count)
            put("averageTimeNanos", node.averageTime)
            if (node.children.isNotEmpty()) {
                put("children", node.children.map(::node))
            }
        }
}
