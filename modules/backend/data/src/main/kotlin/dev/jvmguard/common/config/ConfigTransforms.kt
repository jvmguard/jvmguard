package dev.jvmguard.common.config

import dev.jvmguard.agent.config.transactions.OtelTransactionDef
import dev.jvmguard.agent.config.transactions.TransactionSettings
import dev.jvmguard.data.config.GroupConfig

object ConfigTransforms {

    class Context(
        val findRootGroupConfig: () -> GroupConfig?,
        val storeGroupConfig: (GroupConfig) -> Unit,
    )

    val TRANSFORMS: List<Pair<Int, Context.() -> Boolean>> = listOf(
        1 to { addOtelTransactionDef() }
    )

    // adds the default OTel transaction definition to the root group for installations that
    // predate the OTel transaction type
    private fun Context.addOtelTransactionDef(): Boolean {
        val root = findRootGroupConfig() ?: return false
        val transactionDefs = root.transactionSettings.transactionDefs
        if (transactionDefs.any { it is OtelTransactionDef }) {
            return false
        }
        val otelDef = OtelTransactionDef()
        otelDef.initDefault()
        transactionDefs.add(otelDef)
        TransactionSettings.assignIds(transactionDefs)
        storeGroupConfig(root)
        return true
    }
}
