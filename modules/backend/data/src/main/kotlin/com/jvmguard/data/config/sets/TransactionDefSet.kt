package com.jvmguard.data.config.sets

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.agent.config.transactions.TransactionDef
import com.jvmguard.data.base.StoredType

@StoredType("transaction_def_set")
open class TransactionDefSet : AbstractSet<TransactionDef> {

    @DefaultConstructor
    constructor()

    constructor(name: String, transactionDefs: Collection<TransactionDef>) : super(name, transactionDefs)
}
