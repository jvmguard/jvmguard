package com.jvmguard.ui.server

import com.jvmguard.connector.api.ServerConnection

interface SettingsModeDraft {

    val dirty: Boolean

    fun markDirty()

    fun onDirty(listener: () -> Unit)

    fun persist(connection: ServerConnection)
}
