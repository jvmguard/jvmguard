package dev.jvmguard.data.transactions

import dev.jvmguard.common.transactions.DataAvailability
import dev.jvmguard.data.vmdata.VM

interface TransactionCursor {
    val availability: DataAvailability
    val isLatest: Boolean
    val startTime: Long
    val interval: TransactionTreeInterval
    val vm: VM? // can return null for all VMs

    val gap: Long
}
