package dev.jvmguard.ui.server

import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.config.LdapUserMapping
import dev.jvmguard.data.config.SsoProviderConfig
import dev.jvmguard.data.user.User
import dev.jvmguard.connector.api.ServerConnection

class SettingsDraft(val config: GlobalConfig) : SettingsModeDraft {

    override var dirty = false
        private set

    private var onDirty: (() -> Unit)? = null

    var allTelemetryNodes: List<String> = emptyList()
    var hiddenTelemetries: Set<String>? = null
    val users = StagedListEdits(User::class.java)
    val ldapMappings = StagedListEdits(LdapUserMapping::class.java)
    val ssoProviders = StagedListEdits(SsoProviderConfig::class.java)

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
        if (ssoProviders.hasChanges()) {
            config.ssoConfig.providers = ArrayList(ssoProviders.items())
        }
        connection.setGlobalConfig(config)
        hiddenTelemetries?.let { hidden ->
            connection.setDeclaredTelemetryNodeVisibilities(allTelemetryNodes.associateWith { it !in hidden })
        }
        if (users.hasChanges()) {
            connection.modifyUsers(users.toModification())
        }
    }
}
