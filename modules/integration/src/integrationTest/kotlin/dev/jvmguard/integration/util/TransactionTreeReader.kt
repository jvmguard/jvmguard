package dev.jvmguard.integration.util

import dev.jvmguard.agent.config.transactions.TransactionType
import dev.jvmguard.data.transactions.TransactionTree
import org.jdom2.Element
import java.io.FileInputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

object TransactionTreeReader {

    fun read(fileName: String) = FileInputStream(fileName).use { read(it) }

    fun read(inputStream: InputStream): TransactionTree {
        val rootTree = TransactionTree()
        val docNode = parseXmlString(inputStream.reader().readText())
        val timeUnit = readTimeUnit(docNode)
        fill(rootTree, docNode.children[0], timeUnit)
        return rootTree
    }

    private fun fill(parentTree: TransactionTree, parentNode: Element, timeUnit: TimeUnit) {
        parentNode.all("children").firstOrNull()?.children?.forEach { childNode ->
            val transactionTypeString = childNode.attr("type")
            val type = if (transactionTypeString != null) TransactionType.valueOf(transactionTypeString.uppercase(Locale.getDefault())) else null
            val childTree = parentTree.getOrCreateChild(TransactionTree().init(0, childNode.attr("name"), type ?: TransactionType.MATCHED, getPolicy(childNode)))
            childTree.addCount(requireNotNull(childNode.attr("count")).toLong())
            val time = childNode.attr("time")?.toLong() ?: 0
            childTree.addTime(if (time == -1L) -1L else timeUnit.toNanos(time))

            fill(childTree, childNode, timeUnit)
        }
    }

    private fun getPolicy(node: Element) = when {
        node.attr("error") != null -> node.attr("error")
        else -> Helper.getTypeString(node.attr("policy"))
    }
}

internal fun readTimeUnit(docNode: Element): TimeUnit {
    val timeUnitString = docNode.attr("timeUnit")
    return if (timeUnitString != null) {
        TimeUnit.valueOf(timeUnitString.uppercase(Locale.getDefault()))
    } else {
        TimeUnit.NANOSECONDS
    }
}
