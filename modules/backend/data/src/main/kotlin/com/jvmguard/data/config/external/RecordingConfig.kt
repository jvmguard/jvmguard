package com.jvmguard.data.config.external

import com.grack.nanojson.JsonArray
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonParserException
import com.grack.nanojson.JsonWriter
import com.jvmguard.agent.comm.JsonAgentReader
import com.jvmguard.agent.comm.JsonAgentWriter
import com.jvmguard.agent.config.AgentGroupConfig
import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.tools.importer.ConfigFileFormat
import com.jvmguard.common.config.ConfigStorage
import com.jvmguard.common.helper.GroupHelper
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.ServerGroupConfig
import com.jvmguard.data.vmdata.VmIdentifier
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class RecordingConfig() : ExternalConfig {

    var groupConfigs: MutableCollection<GroupConfig> = ArrayList()
        private set

    constructor(groupConfigs: Collection<GroupConfig>) : this() {
        setGroupConfigs(groupConfigs)
    }

    fun setGroupConfigs(groupConfigs: Collection<GroupConfig>) {
        val sortedGroupConfigs = ArrayList(groupConfigs)
        GroupHelper.sortByHierarchy(sortedGroupConfigs)
        this.groupConfigs = sortedGroupConfigs
    }

    fun toJson(): JsonObject {
        val root = JsonObject()
        root[ConfigFileFormat.KEY_VERSION] = ConfigFileFormat.FILE_VERSION
        root[ConfigFileFormat.KEY_TYPE] = ConfigFileFormat.TYPE_RECORDING_CONFIG
        val groups = JsonArray()
        for (groupConfig in groupConfigs) {
            groups.add(groupToJson(groupConfig))
        }
        root[ConfigFileFormat.KEY_GROUPS] = groups
        return root
    }

    fun export(out: OutputStream) {
        val root = try {
            toJson()
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Could not serialize recording config", e)
        }
        out.write(JsonWriter.string(root).toByteArray(StandardCharsets.UTF_8))
    }

    fun fromJson(root: JsonObject?) {
        root?.getArray(ConfigFileFormat.KEY_GROUPS)?.forEach { element ->
            groupConfigs.add(groupFromJson(element as JsonObject))
        }
    }

    companion object {
        fun groupToJson(groupConfig: GroupConfig): JsonObject {
            val groupEntry = JsonObject()
            groupEntry[ConfigFileFormat.KEY_PATH] = groupConfig.hierarchyPath
            groupEntry[ConfigFileFormat.KEY_GROUP_TYPE] = groupConfig.groupType.databaseId
            if (groupConfig.id != null) {
                groupEntry[ConfigFileFormat.KEY_ID] = groupConfig.id
            }

            val agentConfig = JsonObject()
            groupConfig.agentGroupConfig.writeState(JsonAgentWriter(agentConfig))
            groupEntry[ConfigFileFormat.KEY_AGENT_CONFIG] = agentConfig

            val serverConfigJson = ConfigStorage.objectMapper().writeValueAsString(groupConfig.serverGroupConfig)
            try {
                groupEntry[ConfigFileFormat.KEY_SERVER_CONFIG] = JsonParser.any().from(serverConfigJson)
            } catch (e: JsonParserException) {
                throw IOException("Could not parse serialized server group config", e)
            }

            return groupEntry
        }

        fun groupFromJson(groupEntry: JsonObject): GroupConfig {
            val path = groupEntry.getString(ConfigFileFormat.KEY_PATH, "")
            val groupType = groupEntry.getInt(ConfigFileFormat.KEY_GROUP_TYPE, VmType.GROUP.databaseId)
            val identifier = VmIdentifier(path, VmType.fromDatabaseId(groupType))

            val agentGroupConfig = AgentGroupConfig()
            val agentConfig = groupEntry.getObject(ConfigFileFormat.KEY_AGENT_CONFIG)
            if (agentConfig != null) {
                try {
                    agentGroupConfig.readState(JsonAgentReader(agentConfig))
                } catch (e: Exception) {
                    throw IOException("Could not read agent config", e)
                }
            }

            val serverConfigValue = groupEntry[ConfigFileFormat.KEY_SERVER_CONFIG]
            val serverGroupConfig = if (serverConfigValue != null) {
                ConfigStorage.objectMapper().readValue(JsonWriter.string(serverConfigValue), ServerGroupConfig::class.java)
            } else {
                ServerGroupConfig()
            }

            val groupConfig = GroupConfig(identifier, agentGroupConfig, serverGroupConfig)
            if (groupEntry.has(ConfigFileFormat.KEY_ID) && !groupEntry.isNull(ConfigFileFormat.KEY_ID)) {
                groupConfig.id = groupEntry.getLong(ConfigFileFormat.KEY_ID, 0L)
            }
            return groupConfig
        }
    }
}
