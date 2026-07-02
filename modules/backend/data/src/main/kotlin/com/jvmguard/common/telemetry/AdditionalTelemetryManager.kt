package com.jvmguard.common.telemetry

import com.jvmguard.agent.AgentConstants
import com.jvmguard.agent.telemetry.TelemetryFormatImpl
import com.jvmguard.agent.telemetry.TelemetryHelper
import com.jvmguard.annotation.TelemetryFormat
import com.jvmguard.annotation.Unit
import com.jvmguard.common.Loggers
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import java.io.IOException
import java.sql.ResultSet
import java.sql.SQLException

@Component
@DependsOnDatabaseInitialization
class AdditionalTelemetryManager(private val jdbcClient: JdbcClient) {

    private val additionalTelemetries = HashMap<AdditionalTelemetryIdentifier, AdditionalTelemetry>()
    private val assignedStringIdToAdditionalTelemetry = HashMap<String, AdditionalTelemetry>()
    private val nodeIdToFormat = HashMap<AdditionalTelemetryIdentifier, TelemetryFormat>()

    init {
        try {
            jdbcClient.sql("select * from additional_telemetry").query { resultSet ->
                val additionalTelemetry = AdditionalTelemetry(
                    resultSet.getInt("type"),
                    resultSet.getString("name"),
                    resultSet.getInt("id"),
                    resultSet.getBoolean("hidden"),
                )
                additionalTelemetries[AdditionalTelemetryIdentifier(resultSet.getInt("type"), resultSet.getString("name"))] = additionalTelemetry
                assignedStringIdToAdditionalTelemetry[additionalTelemetry.assignedStringId] = additionalTelemetry
            }
            jdbcClient.sql("select * from additional_telemetry_format").query { resultSet ->
                val nodeId = AdditionalTelemetryIdentifier(resultSet.getInt("type"), resultSet.getString("nodeName"))
                try {
                    getTelemetryFormat(resultSet)?.let { nodeIdToFormat[nodeId] = it }
                } catch (e: IOException) {
                    throw SQLException(e)
                }
            }
        } catch (e: Exception) {
            SERVER_LOGGER.error("could not load additional telemetry", e)
        }
    }

    @Synchronized
    fun getAdditionalTelemetry(type: Int, name: String): AdditionalTelemetry? =
        additionalTelemetries[AdditionalTelemetryIdentifier(type, name)]

    @Synchronized
    fun getOrCreateAdditionalTelemetry(type: Int, name: String): AdditionalTelemetry? {
        val previous = additionalTelemetries[AdditionalTelemetryIdentifier(type, name)]
        if (previous != null) {
            return previous
        }
        val additionalTelemetry = AdditionalTelemetry(type, name, 0, false)
        val keyHolder = GeneratedKeyHolder()
        jdbcClient
            .sql("INSERT INTO additional_telemetry (type, name, description) VALUES (?,?,?)")
            .param(type).param(name).param(additionalTelemetry.description)
            .update(keyHolder)
        val key = keyHolder.key ?: return null
        additionalTelemetry.assignedId = key.toInt()
        additionalTelemetries[AdditionalTelemetryIdentifier(type, name)] = additionalTelemetry
        assignedStringIdToAdditionalTelemetry[additionalTelemetry.assignedStringId] = additionalTelemetry
        return additionalTelemetry
    }

    @Synchronized
    fun getTelemetries(type: Int, onlyVisible: Boolean): List<AdditionalTelemetry> =
        additionalTelemetries.values.filter { it.type == type && (!onlyVisible || !it.isHidden) }

    @Synchronized
    fun getHiddenTelemetries(type: Int): List<AdditionalTelemetry> =
        additionalTelemetries.values.filter { it.type == type && it.isHidden }

    @Synchronized
    fun getVisibleTelemetry(assignedId: String): AdditionalTelemetry? {
        val additionalTelemetry = assignedStringIdToAdditionalTelemetry[assignedId]
        return if (additionalTelemetry == null || additionalTelemetry.isHidden) null else additionalTelemetry
    }

    fun getFormat(additionalTelemetry: AdditionalTelemetry): TelemetryFormat {
        synchronized(this) {
            return nodeIdToFormat[AdditionalTelemetryIdentifier(additionalTelemetry.type, additionalTelemetry.nodeName)]
                ?: TelemetryHelper.DEFAULT_FORMAT
        }
    }

    fun updateFormat(additionalTelemetry: AdditionalTelemetry?, telemetryFormat: TelemetryFormat?): Boolean {
        if (telemetryFormat != null && additionalTelemetry != null) {
            val previousFormat: TelemetryFormat?
            val nodeId = AdditionalTelemetryIdentifier(additionalTelemetry.type, additionalTelemetry.nodeName)
            synchronized(this) {
                previousFormat = nodeIdToFormat.put(nodeId, telemetryFormat)
            }
            if (previousFormat == null || previousFormat.scale != telemetryFormat.scale || previousFormat.stacked != telemetryFormat.stacked ||
                previousFormat.groupAverage != telemetryFormat.groupAverage || previousFormat.value != telemetryFormat.value
            ) {
                try {
                    val unitIntValue = TelemetryHelper.getUnitIntValue(telemetryFormat.value)
                    jdbcClient
                        .sql("MERGE INTO additional_telemetry_format (type, nodeName, unit, scale, stacked, groupAverage) KEY (type, nodeName) VALUES (?,?,?,?,?,?)")
                        .param(nodeId.type).param(nodeId.name).param(unitIntValue)
                        .param(telemetryFormat.scale).param(telemetryFormat.stacked).param(telemetryFormat.groupAverage)
                        .update()
                } catch (e: IOException) {
                    SERVER_LOGGER.error("could not update AdditionalTelemetry format", e)
                }
                return true
            }
        }
        return false
    }

    fun setDevOpsTelemetryHidden(nodeName: String?, hidden: Boolean): Boolean {
        if (nodeName != null) {
            val changedIds = ArrayList<Int>()
            synchronized(this) {
                val nodeNameWithLineStart = "$nodeName\t"
                for (additionalTelemetry in assignedStringIdToAdditionalTelemetry.values) {
                    if (additionalTelemetry.type == AgentConstants.TELEMETRY_TYPE_DEVOPS &&
                        (additionalTelemetry.name == nodeName || additionalTelemetry.name?.startsWith(nodeNameWithLineStart) == true)
                    ) {
                        changedIds.add(additionalTelemetry.assignedId)
                        additionalTelemetry.isHidden = hidden

                        val additionalTelemetry2 = additionalTelemetries[AdditionalTelemetryIdentifier(additionalTelemetry.type, additionalTelemetry.name)]
                        additionalTelemetry2?.isHidden = hidden
                    }
                }
            }

            if (changedIds.isNotEmpty()) {
                for (changedId in changedIds) {
                    jdbcClient.sql("UPDATE additional_telemetry SET hidden=? WHERE id=?")
                        .param(hidden).param(changedId)
                        .update()
                }
                return true
            }
        }
        return false
    }

    companion object {
        private val SERVER_LOGGER = Loggers.SERVER

        private fun getTelemetryFormat(resultSet: ResultSet): TelemetryFormat? {
            var noFormat = true

            var unit = Unit.PLAIN
            val unitIntVal = resultSet.getInt("unit")
            if (!resultSet.wasNull()) {
                unit = TelemetryHelper.getUnitFromIntValue(unitIntVal)
                noFormat = false
            }
            val stacked = resultSet.getBoolean("stacked")
            if (!resultSet.wasNull()) {
                noFormat = false
            }
            var groupAverage = resultSet.getBoolean("groupAverage")
            if (!resultSet.wasNull()) {
                noFormat = false
            } else {
                groupAverage = true
            }
            val scale = resultSet.getInt("scale")
            if (!resultSet.wasNull()) {
                noFormat = false
            }

            return if (noFormat) null else TelemetryFormatImpl(unit, stacked, groupAverage, scale)
        }
    }
}
