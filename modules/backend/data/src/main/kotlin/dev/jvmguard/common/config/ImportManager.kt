package dev.jvmguard.common.config

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import dev.jvmguard.agent.tools.importer.ConfigFileFormat
import dev.jvmguard.common.Loggers
import dev.jvmguard.common.helper.GroupHelper
import dev.jvmguard.common.helper.ListModification
import dev.jvmguard.common.telemetry.AdditionalTelemetryManager
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.external.ExternalConfig
import dev.jvmguard.data.config.external.RecordingConfig
import dev.jvmguard.data.config.external.ServerInitConfig
import dev.jvmguard.data.config.sets.*
import dev.jvmguard.data.config.triggers.ThresholdTrigger
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.User
import dev.jvmguard.data.user.UserManager
import dev.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import org.springframework.stereotype.Component
import java.io.*
import java.nio.charset.StandardCharsets

@Component
class ImportManager(
    private val configManager: ConfigManager,
    private val userManager: UserManager,
    private val configStorage: ConfigStorage,
    private val additionalTelemetryManager: AdditionalTelemetryManager,
) {

    fun importServerInitConfig(serverInitConfig: ServerInitConfig, currentUser: User?) {
        val serverConfig = serverInitConfig.serverConfig

        val oldUsers = ArrayList(userManager.getAllUsers())
        removeCurrentUser(oldUsers, currentUser)

        val newUsers = ArrayList(serverConfig.users!!)
        removeCurrentUser(newUsers, currentUser)

        userManager.modifyUsers(ListModification(emptyList(), oldUsers, newUsers, User::class.java))

        val groupConfigs = configManager.getGroupConfigs(AccessLevel.ADMIN, null)
        configManager.modifyGroupConfigs(
            ListModification(emptyList(), groupConfigs, serverInitConfig.recordingConfig.groupConfigs, GroupConfig::class.java),
            AccessLevel.ADMIN,
            null,
        )

        configManager.setGlobalConfig(serverConfig.globalConfig!!, true)

        configStorage.replaceAll(ActionSet::class.java, serverConfig.actionSets!!)
        configStorage.replaceAll(ThresholdSet::class.java, serverConfig.thresholdSets!!)
        configStorage.replaceAll(TransactionDefSet::class.java, serverConfig.transactionDefSets!!)
        configStorage.replaceAll(TriggerSet::class.java, serverConfig.triggerSets!!)
        configStorage.replaceAll(TelemetrySet::class.java, serverConfig.telemetrySets!!)
    }

    private fun removeCurrentUser(users: MutableCollection<User>, currentUser: User?) {
        if (currentUser != null) {
            for (user in users) {
                if (user.loginName == currentUser.loginName) {
                    users.remove(user)
                    break
                }
            }
        }
    }

    fun importConfig(configFile: File): Boolean =
        try {
            when (val config = readConfig(FileInputStream(configFile))) {
                is RecordingConfig -> importRecordingConfig(config)
                is ServerInitConfig -> importServerInitConfig(config, null)
                else -> throw IOException("Wrong file format")
            }
            true
        } catch (e: Exception) {
            LOGGER.error("Error importing config", e)
            false
        }

    fun readConfig(input: InputStream): ExternalConfig? {
        val root: JsonObject = try {
            InputStreamReader(BufferedInputStream(input), StandardCharsets.UTF_8).use { reader ->
                JsonParser.`object`().from(reader)
            }
        } catch (e: Exception) {
            throw IOException("Could not parse config file as JSON", e)
        }
        val version = root.getInt(ConfigFileFormat.KEY_VERSION, 0)
        if (version == 0) {
            throw IOException("Missing version in config file")
        }
        if (version > ConfigFileFormat.FILE_VERSION) {
            throw IOException("Config file version $version is too new, supported version: " + ConfigFileFormat.FILE_VERSION)
        }
        val type = root.getString(ConfigFileFormat.KEY_TYPE, null)
        try {
            if (ConfigFileFormat.TYPE_SERVER_INIT == type) {
                val serverInitConfig = ServerInitConfig()
                serverInitConfig.fromJson(root)
                updateRecordingConfig(serverInitConfig.recordingConfig)
                return serverInitConfig
            } else if (ConfigFileFormat.TYPE_RECORDING_CONFIG == type) {
                val recordingConfig = RecordingConfig()
                recordingConfig.fromJson(root)
                updateRecordingConfig(recordingConfig)
                return recordingConfig
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Could not read config", e)
        }
        return null
    }

    private fun updateRecordingConfig(recordingConfig: RecordingConfig) {
        for (groupConfig in recordingConfig.groupConfigs) {
            for (trigger in groupConfig.triggerSettings.triggers) {
                if (trigger is ThresholdTrigger) {
                    trigger.thresholdIdentifier!!.telemetryIdentifier =
                        updateTelemetryIdentifier(trigger.thresholdIdentifier!!.telemetryIdentifier)
                }
            }
            for (threshold in groupConfig.thresholdSettings.thresholds) {
                threshold.telemetryIdentifier = updateTelemetryIdentifier(threshold.telemetryIdentifier)
            }
        }
    }

    private fun updateTelemetryIdentifier(telemetryIdentifier: PersistentTelemetryIdentifier?): PersistentTelemetryIdentifier? {
        if (telemetryIdentifier != null && telemetryIdentifier.additionalType != PersistentTelemetryIdentifier.DEFAULT_ADDITIONAL_TYPE) {
            val additionalTelemetry =
                additionalTelemetryManager.getOrCreateAdditionalTelemetry(telemetryIdentifier.additionalType, telemetryIdentifier.additionalName)
            return PersistentTelemetryIdentifier(
                telemetryIdentifier.mainId,
                additionalTelemetry?.assignedStringId,
                telemetryIdentifier.additionalType,
                telemetryIdentifier.additionalName,
            )
        }
        return telemetryIdentifier
    }

    private fun importRecordingConfig(recordingConfig: RecordingConfig) {
        val newGroupConfigs = ArrayList(recordingConfig.groupConfigs)
        GroupHelper.sortByHierarchy(newGroupConfigs)

        val removedGroupConfigs = ArrayList<GroupConfig>()
        val groupConfigs = configManager.getGroupConfigs()
        for (previousConfig in groupConfigs) {
            val previousIdentifier = previousConfig.groupIdentifier
            for (newConfig in newGroupConfigs) {
                if (previousIdentifier.isIncluded(newConfig.groupIdentifier)) {
                    removedGroupConfigs.add(previousConfig)
                    break
                }
            }
        }

        if (newGroupConfigs.isNotEmpty()) {
            val listModification = ListModification(emptyList(), removedGroupConfigs, newGroupConfigs, GroupConfig::class.java)
            configManager.modifyGroupConfigs(listModification, AccessLevel.ADMIN, null)
        }
    }

    companion object {
        const val SERVER_CONFIG_NAME = "jvmguard_server_config"
        const val SERVER_CONFIG_FILE_NAME = "$SERVER_CONFIG_NAME.json"
        const val RECORDING_CONFIG_NAME = "jvmguard_recording_config"
        const val RECORDING_CONFIG_FILE_NAME = "$RECORDING_CONFIG_NAME.json"

        private val LOGGER = Loggers.SERVER
    }
}
