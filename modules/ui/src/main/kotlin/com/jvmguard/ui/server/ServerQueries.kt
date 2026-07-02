package com.jvmguard.ui.server

import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.connector.api.ServerConnection

fun serverTime(): Long =
    Sessions.current()?.serverConnection?.currentTime ?: System.currentTimeMillis()

fun ServerConnection.findVm(selection: VmIdentifier): VM? =
    namedVms.firstOrNull { it.qualifiedIdentifier == selection }
