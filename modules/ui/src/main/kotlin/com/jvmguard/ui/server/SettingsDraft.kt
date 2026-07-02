package com.jvmguard.ui.server

import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.LdapUserMapping
import com.jvmguard.data.user.User
import com.jvmguard.connector.api.ServerConnection

class SettingsDraft(val config: GlobalConfig) : SettingsModeDraft {

    override var dirty = false
        private set

    private var onDirty: (() -> Unit)? = null

    var allTelemetryNodes: List<String> = emptyList()
    var hiddenTelemetries: Set<String>? = null
    val users = StagedListEdits(User::class.java)
    val ldapMappings = StagedListEdits(LdapUserMapping::class.java)

    override fun markDirty() {
        if (!dirty) {
            dirty = true
            onDirty?.invoke()
        }
    }

    override fun onDirty(listener: () -> Unit) {
        onDirty = listener
    }

    override fun persist(connection: ServerConnection) {
        if (ldapMappings.hasChanges()) {
            config.ldapConfig.userMappings = ArrayList(ldapMappings.items())
        }
        connection.setGlobalConfig(config)
        hiddenTelemetries?.let { hidden ->
            connection.setDevOpsTelemetryNodeVisibilities(allTelemetryNodes.associateWith { it !in hidden })
        }
        if (users.hasChanges()) {
            connection.modifyUsers(users.toModification())
        }
    }
}
