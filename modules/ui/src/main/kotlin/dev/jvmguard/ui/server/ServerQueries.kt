package dev.jvmguard.ui.server

import dev.jvmguard.data.vmdata.VM
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.connector.api.ServerConnection

fun serverTime(): Long =
    Sessions.current()?.serverConnection?.currentTime ?: System.currentTimeMillis()

fun ServerConnection.findVm(selection: VmIdentifier): VM? =
    namedVms.firstOrNull { it.qualifiedIdentifier == selection }
