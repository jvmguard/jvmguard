package com.jvmguard.data.config.external

import com.grack.nanojson.JsonArray
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonParserException
import com.grack.nanojson.JsonWriter
import com.jvmguard.agent.tools.importer.ConfigFileFormat
import com.jvmguard.common.config.ConfigStorage
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.sets.ActionSet
import com.jvmguard.data.config.sets.TelemetrySet
import com.jvmguard.data.config.sets.ThresholdSet
import com.jvmguard.data.config.sets.TransactionDefSet
import com.jvmguard.data.config.sets.TriggerSet
import com.jvmguard.data.user.User
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class ServerInitConfig() : ExternalConfig {

    val recordingConfig: RecordingConfig = RecordingConfig()
    var serverConfig: ServerConfig = ServerConfig()
        private set

    constructor(
        globalConfig: GlobalConfig?,
        users: Collection<User>,
        groupConfigs: Collection<GroupConfig>,
        actionSets: Collection<ActionSet>,
        thresholdSets: Collection<ThresholdSet>,
        transactionDefSets: Collection<TransactionDefSet>,
        triggerSets: Collection<TriggerSet>,
        telemetrySets: Collection<TelemetrySet>,
    ) : this() {
        serverConfig.apply {
            this.globalConfig = globalConfig
            this.users = users
            this.actionSets = actionSets
            this.thresholdSets = thresholdSets
            this.transactionDefSets = transactionDefSets
            this.triggerSets = triggerSets
            this.telemetrySets = telemetrySets
        }

        recordingConfig.setGroupConfigs(groupConfigs)
    }

    fun export(out: OutputStream) {
        val root = JsonObject()
        root[ConfigFileFormat.KEY_VERSION] = ConfigFileFormat.FILE_VERSION
        root[ConfigFileFormat.KEY_TYPE] = ConfigFileFormat.TYPE_SERVER_INIT

        val recordingGroups = try {
            recordingConfig.toJson().getArray(ConfigFileFormat.KEY_GROUPS)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Could not serialize recording config", e)
        } ?: JsonArray()
        root[ConfigFileFormat.KEY_GROUPS] = recordingGroups

        val serverConfigJson = ConfigStorage.objectMapper().writeValueAsString(serverConfig)
        try {
            root[ConfigFileFormat.KEY_SERVER_CONFIG] = JsonParser.any().from(serverConfigJson)
        } catch (e: JsonParserException) {
            throw IOException("Could not parse serialized server config", e)
        }

        out.write(JsonWriter.string(root).toByteArray(StandardCharsets.UTF_8))
    }

    fun fromJson(root: JsonObject) {
        recordingConfig.fromJson(root)
        val serverConfigValue = root[ConfigFileFormat.KEY_SERVER_CONFIG]
        if (serverConfigValue != null) {
            serverConfig = ConfigStorage.objectMapper().readValue(JsonWriter.string(serverConfigValue), ServerConfig::class.java)
        }
    }
}
