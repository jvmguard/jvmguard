package dev.jvmguard.ui.server

import dev.jvmguard.common.helper.ListModification
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.connector.api.ServerConnection

class RecordingDraft(val groupConfigs: List<GroupConfig>) : SettingsModeDraft {

    override var dirty = false
        private set

    private var onDirty: (() -> Unit)? = null
    private val changedGroups = mutableSetOf<VmIdentifier>()
    private val byIdentifier: Map<VmIdentifier, GroupConfig> = groupConfigs.associateBy { it.groupIdentifier }

    fun groupConfig(identifier: VmIdentifier): GroupConfig? = byIdentifier[identifier]

    fun markChanged(identifier: VmIdentifier) {
        changedGroups.add(identifier)
        markDirty()
    }

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
        val modified = groupConfigs.filter { it.groupIdentifier in changedGroups }
        if (modified.isEmpty()) {
            return
        }
        connection.modifyGroupConfigs(
            ListModification(modified, emptyList(), emptyList(), GroupConfig::class.java),
        )
    }
}
