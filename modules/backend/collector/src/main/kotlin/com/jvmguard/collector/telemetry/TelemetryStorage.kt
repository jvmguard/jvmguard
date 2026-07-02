package com.jvmguard.collector.telemetry

import com.jvmguard.agent.comm.JvmGuardCommunication
import com.jvmguard.annotation.MethodTransaction
import com.jvmguard.annotation.Part
import com.jvmguard.collector.main.VmRegistry
import com.jvmguard.collector.util.Consolidator
import com.jvmguard.collector.util.Consolidator.TimeProvider
import com.jvmguard.collector.vmdata.AbstractVmData.TelemetryDataVisitor
import com.jvmguard.common.Loggers
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.common.helper.DatabaseHelper
import com.jvmguard.data.vmdata.TelemetryIdentifier
import com.jvmguard.data.vmdata.VM
import jakarta.annotation.PostConstruct
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource

@Component
@DependsOnDatabaseInitialization
class TelemetryStorage(
    private val telemetryIdentifierLists: TelemetryIdentifierLists,
    private val vmRegistry: VmRegistry,
    private val consolidator: Consolidator,
    private val dataSource: DataSource,
    private val jdbcClient: JdbcClient,
    private val properties: JvmGuardProperties,
) {

    @PostConstruct
    fun postConstruct() {
        TelemetryDataInterval.applyStorageOverrides(properties.telemetryStorage)
        try {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    for (interval in TelemetryDataInterval.getDatabaseIntervals()) {
                        DatabaseHelper.createTableIfNotExists(
                            statement,
                            interval.tableName + "(vmId BIGINT NOT NULL, dataTime BIGINT NOT NULL, listId INT NOT NULL, version INT NOT NULL, content MEDIUMBLOB NOT NULL, PRIMARY KEY(dataTime, vmId))"
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            SERVER_LOGGER.error("error creating telemetry tables", e)
        }

        for (interval in TelemetryDataInterval.getDatabaseIntervals()) {
            val storageTime = interval.storageMillis
            if (storageTime < Long.MAX_VALUE) {
                consolidator.register(interval.tableName, "dataTime", 12, object : TimeProvider() {
                    override fun getMillis(): Long = storageTime
                })
            }
        }
    }

    private fun getBytes(values: LongArray): ByteArray {
        val ret = ByteArray(values.size * 8)
        ByteBuffer.wrap(ret).asLongBuffer().put(values)
        return ret
    }

    private fun readValues(@Suppress("unused") version: Int, data: ByteArray): LongArray {
        val ret = LongArray(data.size / 8)
        ByteBuffer.wrap(data).asLongBuffer().get(ret)
        return ret
    }

    fun createInitialDataTask(timeStamp: Long): TelemetryWriteTask {
        val dataInterval = TelemetryDataInterval.getDatabaseIntervals()[0]
        val entries = ArrayList<TelemetryEntry>()

        vmRegistry.rootVmGroupData.visitTelemetryData(object : TelemetryDataVisitor {
            override fun initListId(telemetryIdentifiers: List<TelemetryIdentifier>): Int? =
                telemetryIdentifierLists.getId(telemetryIdentifiers)

            override fun visit(listId: Int?, vm: VM, values: LongArray) {
                entries.add(TelemetryEntry(vm.id, listId!!, getBytes(values)))
            }
        }, dataInterval)

        return TelemetryWriteTask { connection -> storeInitialData(connection, dataInterval, timeStamp, entries) }
    }

    @MethodTransaction(naming = [Part(text = "store initial telemetry data")])
    fun storeInitialData(connection: Connection, dataInterval: TelemetryDataInterval, timeStamp: Long, entries: List<TelemetryEntry>) {
        @Suppress("SqlSourceToSinkFlow")
        val statement = connection.prepareStatement(DatabaseHelper.getMergeInto(dataInterval.tableName, "vmId", "dataTime", "listId", "version", "content"))
        statement.setLong(2, timeStamp)
        statement.setInt(4, JvmGuardCommunication.PROTOCOL_VERSION)

        for (entry in entries) {
            statement.setLong(1, entry.vmId)
            statement.setInt(3, entry.listId)
            statement.setBytes(5, entry.data)
            statement.execute()
        }

        statement.close()
    }

    fun createConsolidationTask(creationInterval: TelemetryDataInterval, sourceInterval: TelemetryDataInterval, storageTime: Long): TelemetryWriteTask =
        TelemetryWriteTask { connection -> consolidate(connection, creationInterval, sourceInterval, storageTime) }

    @MethodTransaction(naming = [Part(text = "store consolidation telemetry data")])
    fun consolidate(connection: Connection, creationInterval: TelemetryDataInterval, sourceInterval: TelemetryDataInterval, storageTime: Long) {
        @Suppress("SqlSourceToSinkFlow")
        connection.prepareStatement(DatabaseHelper.getMergeInto(creationInterval.tableName, "vmId", "dataTime", "listId", "version", "content"))
            .use { creationStatement ->
                connection.prepareStatement("select vmId, listId, version, content from " + sourceInterval.tableName + " where dataTime>=? and dataTime<? order by vmId")
                    .use { sourceStatement ->
                        creationStatement.setLong(2, storageTime)
                        creationStatement.setInt(4, JvmGuardCommunication.PROTOCOL_VERSION)

                        sourceStatement.setLong(1, storageTime - creationInterval.millis)
                        sourceStatement.setLong(2, storageTime)

                        var lastVmId = 0L
                        var lastListId = 0
                        var currentIds: List<TelemetryIdentifier>? = null
                        val counterMap = TreeMap<TelemetryIdentifier, ConsolidationCounter>()

                        sourceStatement.executeQuery().use { resultSet ->
                            while (resultSet.next()) {
                                val vmId = resultSet.getLong(1)

                                if (lastVmId != 0L && vmId != lastVmId) {
                                    storeConsolidationPoint(creationStatement, lastVmId, counterMap)
                                }

                                val listId = resultSet.getInt(2)
                                if (listId != lastListId) {
                                    currentIds = telemetryIdentifierLists.getIdentifiers(listId)
                                }

                                countValues(currentIds, counterMap, readValues(resultSet.getInt(3), resultSet.getBytes(4)))

                                lastVmId = vmId
                                lastListId = listId
                            }
                        }

                        if (lastVmId != 0L) {
                            storeConsolidationPoint(creationStatement, lastVmId, counterMap)
                        }
                    }
            }
    }

    private fun countValues(currentIds: List<TelemetryIdentifier>?, counterMap: MutableMap<TelemetryIdentifier, ConsolidationCounter>, values: LongArray) {
        if (currentIds != null && currentIds.size == values.size) { // should always be the case
            for (i in values.indices) {
                val telemetryIdentifier = currentIds[i]
                val consolidationCounter = counterMap.computeIfAbsent(telemetryIdentifier) { _ -> ConsolidationCounter() }
                consolidationCounter.count(values[i])
            }
        }
    }

    private fun storeConsolidationPoint(creationStatement: PreparedStatement, vmId: Long, counterMap: MutableMap<TelemetryIdentifier, ConsolidationCounter>) {
        val identifiers = ArrayList(counterMap.keys)
        val values = LongArray(identifiers.size)
        var index = 0
        for (counter in counterMap.values) {
            values[index++] = counter.average
        }
        counterMap.clear()

        creationStatement.setLong(1, vmId)
        creationStatement.setInt(3, telemetryIdentifierLists.getId(identifiers)!!)
        creationStatement.setBytes(5, getBytes(values))
        creationStatement.execute()
    }

    fun deleteVMs(connection: Connection, vms: List<VM>) {
        for (interval in TelemetryDataInterval.getDatabaseIntervals()) {
            val statement = connection.prepareStatement("DELETE FROM " + interval.tableName + " WHERE vmId=?")
            for (vm in vms) {
                statement.setLong(1, vm.id)
                statement.execute()
            }
            statement.close()
        }
    }

    fun visitData(dataInterval: TelemetryDataInterval, vm: VM?, startTimeInclusive: Long, endTimeExclusive: Long, visitor: DataVisitor) {
        val sql =
            "select dataTime, listId, version, content, vmId from " + dataInterval.tableName + " where dataTime>=? and dataTime<? " + (if (vm == null) " order by vmId" else "and vmId=?")
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement(sql).use { statement ->
                    statement.setLong(1, startTimeInclusive)
                    statement.setLong(2, endTimeExclusive)
                    if (vm != null) {
                        statement.setLong(3, vm.id)
                    }

                    statement.executeQuery().use { resultSet ->
                        var lastListId = 0
                        var lastVmId = 0L

                        while (resultSet.next()) {
                            val listId = resultSet.getInt(2)
                            val vmId = resultSet.getLong(5)
                            if (vmId != lastVmId) {
                                visitor.visitVmId(vmId)
                                lastVmId = vmId
                            }
                            if (listId != lastListId) {
                                visitor.visitTelemetryIds(telemetryIdentifierLists.getIdentifiers(listId))
                                lastListId = listId
                            }
                            visitor.visitValues(resultSet.getLong(1), readValues(resultSet.getInt(3), resultSet.getBytes(4)))
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            SERVER_LOGGER.error("error visiting telemetry data", e)
        }
        visitor.endVisit()
    }

    fun hasData(dataInterval: TelemetryDataInterval, vm: VM?, endTimeExclusive: Long): Boolean {
        @Suppress("SqlSignature")
        val sql =
            "select case when exists (select * from " + dataInterval.tableName + " where dataTime<?" + (if (vm == null) "" else " and vmId=?") + ") then 1 else 0 end"
        return try {
            var query = jdbcClient.sql(sql).param(endTimeExclusive)
            if (vm != null) {
                query = query.param(vm.id)
            }
            val ret = query
                .query(Int::class.javaObjectType)
                .optional()
                .orElse(null)
            ret != null && ret != 0
        } catch (e: Exception) {
            SERVER_LOGGER.error("error checking previous telemetry data", e)
            false
        }
    }

    fun interface TelemetryWriteTask {
        fun run(connection: Connection)
    }

    interface DataVisitor {
        fun visitVmId(vmId: Long)
        fun visitTelemetryIds(identifiers: List<TelemetryIdentifier>?)
        fun visitValues(timeStamp: Long, values: LongArray)
        fun endVisit()
    }

    class TelemetryEntry(val vmId: Long, val listId: Int, val data: ByteArray)

    private class ConsolidationCounter {
        private var sum: Long = 0
        private var count: Int = 0

        val average: Long
            get() = if (count == 0) Long.MIN_VALUE else sum / count

        fun count(value: Long) {
            if (value > Long.MIN_VALUE) {
                sum += value
                count++
            }
        }
    }

    companion object {
        private val SERVER_LOGGER = Loggers.SERVER
    }
}
