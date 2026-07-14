package dev.jvmguard.data.config.sets

import dev.jvmguard.agent.config.base.DefaultConstructor
import dev.jvmguard.agent.config.transactions.TransactionDef
import dev.jvmguard.data.base.StoredType

@StoredType("transaction_def_set")
open class TransactionDefSet : AbstractSet<TransactionDef> {

    @DefaultConstructor
    constructor()

    constructor(name: String, transactionDefs: Collection<TransactionDef>) : super(name, transactionDefs)
}
