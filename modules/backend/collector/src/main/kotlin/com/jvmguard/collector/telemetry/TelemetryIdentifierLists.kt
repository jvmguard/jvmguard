package com.jvmguard.collector.telemetry

import com.jvmguard.agent.comm.JvmGuardCommunication
import com.jvmguard.collector.main.VmManagerImpl
import com.jvmguard.common.Loggers
import com.jvmguard.data.vmdata.TelemetryIdentifier
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import jakarta.annotation.PostConstruct
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

@Component
@DependsOnDatabaseInitialization
class TelemetryIdentifierLists(private val jdbcClient: JdbcClient) {

    private val telemetryListToDatabaseId = HashMap<List<TelemetryIdentifier>, Int>()
    private val databaseIdToTelemetryList = Int2ObjectOpenHashMap<List<TelemetryIdentifier>>()

    @PostConstruct
    @Synchronized
    fun postConstruct() {
        try {
            jdbcClient
                .sql("select id, version, content from telemetry_list_ids")
                .query { resultSet ->
                    try {
                        readTelemetryIds(resultSet.getInt(1), resultSet.getInt(2), DataInputStream(resultSet.getBinaryStream(3)))
                    } catch (e: IOException) {
                        VmManagerImpl.SERVER_LOGGER.error("reading telemetry ids", e)
                    }
                }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error loading telemetry_list_ids", e)
        }
    }

    @Synchronized
    fun getIdentifiers(id: Int): List<TelemetryIdentifier>? = databaseIdToTelemetryList.get(id)

    @Synchronized
    fun getId(list: List<TelemetryIdentifier>): Int? {
        val existing = telemetryListToDatabaseId[list]
        if (existing != null) {
            return existing
        }
        return try {
            val keyHolder = GeneratedKeyHolder()
            jdbcClient
                .sql("insert into telemetry_list_ids(version, content) values (?,?)")
                .param(JvmGuardCommunication.PROTOCOL_VERSION)
                .param(getBytes(list))
                .update(keyHolder)
            val key = keyHolder.key ?: return null
            val id = key.toInt()
            telemetryListToDatabaseId[list] = id
            databaseIdToTelemetryList.put(id, list)
            id
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error getting telemetry list id", e)
            null
        }
    }

    private fun getBytes(list: List<TelemetryIdentifier>): ByteArray {
        val bout = ByteArrayOutputStream()
        val out = DataOutputStream(bout)
        try {
            for (telemetryIdentifier in list) {
                out.writeBoolean(true)
                out.writeUTF(telemetryIdentifier.mainId)
                out.writeUTF(telemetryIdentifier.subId)
            }
            out.writeBoolean(false)
        } catch (e: IOException) {
            Loggers.SERVER.error("Could not serialize telemetry identifiers", e)
        }
        return bout.toByteArray()
    }

    private fun readTelemetryIds(databaseId: Int, @Suppress("unused") version: Int, `in`: DataInputStream) {
        val list = ArrayList<TelemetryIdentifier>()
        while (`in`.readBoolean()) {
            val mainId = `in`.readUTF()
            val subId = `in`.readUTF()
            list.add(TelemetryIdentifier(mainId, subId))
        }
        telemetryListToDatabaseId[list] = databaseId
        databaseIdToTelemetryList.put(databaseId, list)
    }
}
