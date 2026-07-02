package com.jvmguard.data.transactions

import com.jvmguard.common.transactions.DataAvailability
import com.jvmguard.data.vmdata.VM

interface TransactionCursor {
    val availability: DataAvailability
    val isLatest: Boolean
    val startTime: Long
    val interval: TransactionTreeInterval
    val vm: VM? // can return null for all VMs

    val gap: Long
}
