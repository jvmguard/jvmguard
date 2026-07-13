package com.jvmguard.common.config

import com.jvmguard.agent.config.VmType
import com.jvmguard.common.helper.DeepCopy
import com.jvmguard.common.helper.GroupHelper
import com.jvmguard.common.helper.ListModification
import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.GroupHierarchyWrapper
import com.jvmguard.data.config.SsoPreset
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.LinkedHashSet
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

@Component
class ConfigManager(private val configStorage: ConfigStorage) {

    private val logger = LoggerFactory.getLogger(ConfigManager::class.java)

    private var globalConfig: GlobalConfig? = null
    private val idToGroupConfig: MutableMap<Long, GroupConfig> = ConcurrentHashMap()

    private val configChangeListeners: MutableSet<ConfigChangeListener> = LinkedHashSet()

    init {
        init()
    }

    fun init() {
        initGlobalConfig()
        applyEnvVarOverrides()
        initGroupConfigs()
    }

    fun addConfigChangeListener(listener: ConfigChangeListener) {
        synchronized(configChangeListeners) {
            configChangeListeners.add(listener)
        }
    }

    @Synchronized
    fun getGlobalConfig(obfuscated: Boolean): GlobalConfig =
        if (obfuscated) {
            globalConfig!!.toObfuscatedConfig()
        } else {
            globalConfig!!
        }

    fun setGlobalConfig(globalConfig: GlobalConfig, obfuscated: Boolean) {
        val oldConfig: GlobalConfig?
        val newConfig: GlobalConfig
        synchronized(this) {
            oldConfig = this.globalConfig
            if (oldConfig != null) {
                globalConfig.id = oldConfig.id
            }
            newConfig = DeepCopy.clone(globalConfig)
            normalizeSsoProviders(newConfig)
            this.globalConfig = newConfig

            configStorage.store(GlobalConfig::class.java, if (obfuscated) newConfig else getGlobalConfig(true))
            if (obfuscated) {
                newConfig.deobfuscate()
            }
        }

        synchronized(configChangeListeners) {
            for (listener in configChangeListeners) {
                listener.globalConfigChanged(oldConfig, newConfig)
            }
        }
    }

    fun groupConnected(groupIdentifier: VmIdentifier?) {
        synchronized(idToGroupConfig) {
            if (groupIdentifier != null && !groupIdentifier.isRoot && findGroupConfigByIdentifier(groupIdentifier) == null) {
                modifyConfig(idToGroupConfig, GroupConfig::class.java, GroupConfig.createDefault(groupIdentifier))
            }
        }
    }

    fun getGroupHierarchyWrapper(vm: VM): GroupHierarchyWrapper =
        getGroupHierarchyWrapper(vm.parentIdentifier)

    fun getGroupHierarchyWrapper(groupIdentifier: VmIdentifier): GroupHierarchyWrapper {
        var identifier = groupIdentifier
        val groupConfigPath = LinkedList<GroupConfig>()
        var groupConfig = getGroupConfig(identifier)
        groupConfigPath.add(groupConfig)
        while (!groupConfig.isRoot) {
            identifier = identifier.parent!!
            groupConfig = getGroupConfig(identifier)
            groupConfigPath.add(groupConfig)
        }
        return GroupHierarchyWrapper(groupConfigPath)
    }

    fun getGroupConfig(vmIdentifier: VmIdentifier?): GroupConfig {
        val identifier = vmIdentifier ?: VmIdentifier.ROOT_GROUP_IDENTIFIER
        return findGroupConfigByIdentifier(identifier) ?: getGroupConfig(identifier.parent)
    }

    fun findGroupConfigByIdentifier(vmIdentifier: VmIdentifier): GroupConfig? {
        synchronized(idToGroupConfig) {
            return idToGroupConfig.values.firstOrNull { it.groupIdentifier == vmIdentifier }
        }
    }

    fun getGroupConfigs(): List<GroupConfig> {
        synchronized(idToGroupConfig) {
            val groupConfigs = ArrayList(idToGroupConfig.values)
            GroupHelper.sortByHierarchy(groupConfigs)
            return groupConfigs
        }
    }

    fun getGroupConfigs(accessLevel: AccessLevel, groupNames: List<String>?): List<GroupConfig> {
        if (accessLevel != AccessLevel.ADMIN) {
            val groupRoots = getGroupRoots(groupNames)
            val filteredGroupConfigs = ArrayList<GroupConfig>()
            synchronized(idToGroupConfig) {
                for (groupConfig in idToGroupConfig.values) {
                    if (GroupHelper.checkAgainstGroupRoots(groupConfig, groupRoots)) {
                        filteredGroupConfigs.add(groupConfig)
                    }
                }
            }
            GroupHelper.sortByHierarchy(filteredGroupConfigs)
            return filteredGroupConfigs
        } else {
            return getGroupConfigs()
        }
    }

    fun checkGroupModificationRights(vmIdentifier: VmIdentifier, accessLevel: AccessLevel, groupNames: List<String>?) {
        if (accessLevel != AccessLevel.ADMIN) {
            val groupRoots = getGroupRoots(groupNames)
            checkAgainstGroupRoots(vmIdentifier, groupRoots)
        }
    }

    fun modifyGroupConfigs(listModification: ListModification<GroupConfig>, accessLevel: AccessLevel, groupNames: List<String>?) {
        if (accessLevel != AccessLevel.ADMIN) {
            val groupRoots = getGroupRoots(groupNames)
            checkAgainstGroupRoots(listModification.modifiedItems, groupRoots)
            checkAgainstGroupRoots(listModification.removedItems, groupRoots)
            checkAgainstGroupRoots(listModification.newItems, groupRoots)
        }

        synchronized(idToGroupConfig) {
            modifyConfigs(listModification, idToGroupConfig, GroupConfig::class.java)
        }
        synchronized(configChangeListeners) {
            for (listener in configChangeListeners) {
                listener.groupConfigsChanged()
            }
        }
    }

    fun getGroupRoots(groupNames: List<String>?): List<VmIdentifier> {
        val roots = ArrayList<VmIdentifier>()
        synchronized(idToGroupConfig) {
            for (groupConfig in idToGroupConfig.values) {
                if (groupNames != null && groupNames.contains(groupConfig.hierarchyPath)) {
                    roots.add(groupConfig.groupIdentifier)
                }
            }
        }
        return roots
    }

    private fun checkAgainstGroupRoots(groupConfigs: Collection<GroupConfig>, groupRoots: List<VmIdentifier>) {
        for (groupConfig in groupConfigs) {
            checkAgainstGroupRoots(groupConfig.groupIdentifier, groupRoots)
        }
    }

    private fun checkAgainstGroupRoots(vmIdentifier: VmIdentifier, groupRoots: List<VmIdentifier>) {
        if (!GroupHelper.checkAgainstGroupRoots(vmIdentifier, groupRoots)) {
            throw SecurityException("Insufficient rights for modifying group config $vmIdentifier")
        }
    }

    @Synchronized
    private fun initGlobalConfig() {
        val globalConfigs = configStorage.list(GlobalConfig::class.java)
        if (globalConfigs.isEmpty()) {
            val config = GlobalConfig()
            globalConfig = config
            configStorage.store(GlobalConfig::class.java, config)
        } else {
            val config = globalConfigs.first()
            globalConfig = config
            config.deobfuscate()
            if (normalizeSsoProviders(config)) {
                configStorage.store(GlobalConfig::class.java, config.toObfuscatedConfig())
            }
        }
    }

    private fun normalizeSsoProviders(config: GlobalConfig): Boolean {
        var changed = false
        for (provider in config.ssoConfig.providers) {
            if (provider.preset == SsoPreset.GOOGLE_WORKSPACE && provider.issuerUri.isBlank()) {
                provider.issuerUri = SsoPreset.defaultIssuer(provider.preset)
                changed = true
            }
        }
        return changed
    }

    private fun applyEnvVarOverrides() {
        val config = globalConfig ?: return

        System.getenv("JVMGUARD_LDAP_PASSWORD")?.let {
            logger.info("LDAP bind password overridden by environment variable")
            config.ldapConfig.password = it
        }
    }

    private fun initGroupConfigs() {
        synchronized(idToGroupConfig) {
            idToGroupConfig.clear()
            val groupConfigs = configStorage.list(GroupConfig::class.java)
            for (groupConfig in groupConfigs) {
                idToGroupConfig[groupConfig.id] = groupConfig
            }

            if (findGroupConfigByIdentifier(VmIdentifier.ROOT_GROUP_IDENTIFIER) == null) {
                modifyConfig(idToGroupConfig, GroupConfig::class.java, GroupConfig.createDefault())
            }
        }
    }

    private fun <T : StoredConfig> modifyConfigs(listModification: ListModification<T>, idToConfig: MutableMap<Long, T>, configClass: Class<T>) {
        for (item in listModification.removedItems) {
            val id = item.id
            if (id != null) {
                val config = idToConfig[id]
                if (config != null) {
                    idToConfig.remove(id)
                    configStorage.remove(configClass, id)
                }
            }
        }

        for (item in sort(listModification.modifiedItems)) {
            modifyConfig(idToConfig, configClass, item)
        }

        for (item in sort(listModification.newItems)) {
            modifyConfig(idToConfig, configClass, item)
        }
    }

    // lock for idToGroupConfig held by invoking method, same as idToBean
    private fun <T : StoredConfig> modifyConfig(idToConfig: MutableMap<Long, T>, configClass: Class<T>, config: T) {
        configStorage.store(configClass, config)
        if (config.id != null) {
            idToConfig[config.id] = config
        }

        if (config is GroupConfig) {
            if (config.groupIdentifier != VmIdentifier.ROOT_GROUP_IDENTIFIER) {
                // insert missing parent groups in the hierarchy
                var hierarchyPath = GroupHelper.getParentHierarchyPath(config.hierarchyPath)
                if (hierarchyPath == null) {
                    hierarchyPath = GroupHelper.ROOT_GROUP_ID
                }
                val groupIdentifier = VmIdentifier(hierarchyPath, VmType.GROUP)
                if (GroupHelper.findGroupConfigByGroupIdentifier(groupIdentifier, idToGroupConfig.values) == null) {
                    val groupConfig = GroupConfig.createDefault(groupIdentifier)
                    modifyConfig(idToGroupConfig, GroupConfig::class.java, groupConfig)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : StoredConfig> sort(items: Collection<T>): Collection<T> {
        val groupConfigs = ArrayList(items as Collection<GroupConfig>)
        GroupHelper.sortByHierarchy(groupConfigs)
        return groupConfigs as Collection<T>
    }
}
