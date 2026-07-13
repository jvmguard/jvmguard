package com.jvmguard.collector.transactions

import com.jvmguard.agent.comm.CommandType
import com.jvmguard.agent.comm.JvmGuardCommunication
import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.config.transactions.TransactionType
import com.jvmguard.agent.data.DataSetResult
import com.jvmguard.agent.parameter.DataSetParameter
import com.jvmguard.agent.tree.AbstractTransactionTree
import com.jvmguard.agent.tree.AbstractTransactionTree.PolicyType
import com.jvmguard.agent.util.JvmGuardUtil
import com.jvmguard.annotation.MethodTransaction
import com.jvmguard.annotation.Part
import com.jvmguard.collector.connection.AgentConnectionImpl.Handler
import com.jvmguard.collector.main.CollectorContext
import com.jvmguard.collector.main.VmManagerImpl
import com.jvmguard.collector.telemetry.CollectionTypeResolver
import com.jvmguard.collector.transactions.util.*
import com.jvmguard.collector.transactions.util.AbstractNameManager.NameWriteStatements
import com.jvmguard.collector.transactions.util.Statements.ReadStatementPair
import com.jvmguard.collector.transactions.util.Statements.ReadStatements
import com.jvmguard.collector.util.CurrentConnectionEntry
import com.jvmguard.collector.vmdata.AbstractVmData
import com.jvmguard.common.DatabaseWriter
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.helper.DatabaseHelper
import com.jvmguard.common.transactions.TransactionCursorImpl
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.transactions.CapType
import com.jvmguard.data.transactions.TransactionDataInterval
import com.jvmguard.data.transactions.TransactionDataInterval.IntervalWithTimestamp
import com.jvmguard.data.transactions.TransactionDataInterval.UsedIntervals
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTree
import com.jvmguard.data.vmdata.Telemetry
import com.jvmguard.data.vmdata.TelemetryIdentifier
import com.jvmguard.data.vmdata.TelemetryType
import com.jvmguard.data.vmdata.VM
import it.unimi.dsi.fastutil.longs.LongCollection
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import jakarta.annotation.PostConstruct
import org.jooq.lambda.fi.util.function.CheckedBiConsumer
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.core.env.Environment
import org.springframework.core.env.getProperty
import org.springframework.stereotype.Component
import java.io.*
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("SqlResolve")
// Named to avoid colliding with Spring Boot's autoconfigured "transactionManager" (DataSourceTransactionManager) bean.
@Component("jvmguardTransactionManager")
@DependsOnDatabaseInitialization
class TransactionManager(
    private val collectorContext: CollectorContext,
    private val collectionTypeResolver: CollectionTypeResolver,
    private val configManager: ConfigManager,
    private val databaseWriter: DatabaseWriter,
    private val properties: JvmGuardProperties,
    private val environment: Environment,
    private val dataSource: javax.sql.DataSource,
) {

    private lateinit var nameManager: TypedNameManager

    private val transactionCapWarningShown = AtomicBoolean()

    @Volatile
    private var intervalToStorageMillis: EnumMap<TransactionDataInterval, StorageMillis> = EnumMap(TransactionDataInterval::class.java)

    @PostConstruct
    fun postConstruct() {
        try {
            dataSource.connection.use { connection ->
                nameManager = TypedNameManager(connection, "transaction", ReadEverythingBlock(), properties)

                connection.createStatement().use { statement ->
                    for (interval in TransactionDataInterval.entries) {
                        for (transactionDataType in TransactionDataType.entries) {
                            DatabaseHelper.createTableIfNotExists(
                                statement,
                                interval.getTableName(transactionDataType) + "(vmId BIGINT NOT NULL, snapshotTime BIGINT NOT NULL, groupNode TINYINT NOT NULL, realInterval INT, version INT, content LONGBLOB, PRIMARY KEY(snapshotTime, vmId))"
                            )
                        }
                        DatabaseHelper.createTableIfNotExists(
                            statement,
                            "transaction_consolidation (type VARCHAR(255) PRIMARY KEY, consolidationTime BIGINT NOT NULL)"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error creating transaction tables", e)
        }
        updateGlobalConfig(configManager.getGlobalConfig(false))
    }

    fun updateGlobalConfig(globalConfig: GlobalConfig) {
        updateRetentionTimes(globalConfig)
        updateCaps(globalConfig)
    }

    private fun updateCaps(globalConfig: GlobalConfig) {
        nameManager.setCap(NameType.TRANSACTION, globalConfig.transactionCap)
        nameManager.setCap(NameType.PAYLOAD, globalConfig.payloadCap)
    }

    private fun updateRetentionTimes(globalConfig: GlobalConfig) {
        val intervalToStorageMillis = EnumMap<TransactionDataInterval, StorageMillis>(TransactionDataInterval::class.java)
        val retentionDays = GlobalConfig.getUsedRetentionDays(globalConfig.transactionDays)
        val retentionHours = if (retentionDays == Integer.MAX_VALUE) Integer.MAX_VALUE else (24 * retentionDays + 12)

        putTransactionMillis(intervalToStorageMillis, TransactionDataInterval.MINUTE, 27, 27, 12)
        putTransactionMillis(
            intervalToStorageMillis,
            TransactionDataInterval.HOUR,
            24 * minOf(30, retentionDays),
            24 * minOf(10, retentionDays),
            24 * minOf(10, retentionDays)
        )
        putTransactionMillis(
            intervalToStorageMillis,
            TransactionDataInterval.DAY,
            retentionHours,
            24 * minOf(120, retentionDays) + 12,
            24 * minOf(60, retentionDays) + 12
        )

        this.intervalToStorageMillis = intervalToStorageMillis
    }

    private fun putTransactionMillis(
        intervalToStorageMillis: EnumMap<TransactionDataInterval, StorageMillis>,
        interval: TransactionDataInterval,
        storageHourGroup: Int,
        storageHourVm: Int,
        storageHourPooledVm: Int
    ) {
        intervalToStorageMillis[interval] = StorageMillis(
            getMillis(interval, storageHourGroup, "Group"),
            getMillis(interval, storageHourVm, "Vm"),
            getMillis(interval, storageHourPooledVm, "Pooled")
        )
    }

    private fun getMillis(interval: TransactionDataInterval, storageHours: Int, propertySuffix: String): Long {
        val hours = environment.getProperty<Int>("jvmguard.transactionStorage" + propertySuffix + JvmGuardUtil.getCommandLineName(interval, true), storageHours)
        return if (hours == Integer.MAX_VALUE) {
            Long.MAX_VALUE
        } else {
            hours.toLong() * 60 * 60 * 1000
        }
    }

    val caps: EnumSet<CapType>
        get() {
            val result = EnumSet.noneOf(CapType::class.java)
            if (nameManager.isCapped(NameType.TRANSACTION)) {
                result.add(CapType.TRANSACTION)
            }
            if (nameManager.isCapped(NameType.PAYLOAD)) {
                result.add(CapType.PAYLOAD)
            }
            return result
        }

    fun resetCapCount(ifCappedOnly: Boolean) {
        transactionCapWarningShown.set(false)

        try {
            dataSource.connection.use { connection ->
                nameManager.resetCapCount(NameType.TRANSACTION, ifCappedOnly, connection)
                if (!ifCappedOnly) {
                    nameManager.resetCapCount(NameType.PAYLOAD, false, connection)
                }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error consolidating transactions", e)
        }
    }

    fun initAdditionalIntervals(): ZonedDateTime {
        val now = ZonedDateTime.now()

        try {
            dataSource.connection.use { connection ->
                VmManagerImpl.SERVER_LOGGER.info("Checking transaction consolidation")
                val generationCount = HashMap<TransactionDataInterval, Int>()

                connection.prepareStatement("select consolidationTime from transaction_consolidation where type=?").use { consolidationInfoStatement ->
                    for (destinationInterval in TransactionDataInterval.getCalculatedIntervals()) {
                        val previousInterval = destinationInterval.previousInterval ?: continue
                        var lastTime = 0L
                        consolidationInfoStatement.setString(1, destinationInterval.name)
                        consolidationInfoStatement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                lastTime = resultSet.getLong(1)
                            }
                        }

                        var newestTime = destinationInterval.getFloorStartTime(now)
                        val existingNewestTime = destinationInterval.getFloorStartTime(getTimeExtremum("max", connection, previousInterval))
                        if (existingNewestTime < newestTime) {
                            newestTime =
                                destinationInterval.getNextStartTime(existingNewestTime) // newestTime is excluded from generation, so it must be advanced by one to include all existing data
                        }

                        var currentStartTime: Long
                        if (lastTime == 0L) {
                            val oldestTime = getTimeExtremum("min", connection, previousInterval)
                            currentStartTime = if (oldestTime != 0L) destinationInterval.getFloorStartTime(oldestTime) else newestTime
                        } else {
                            currentStartTime = destinationInterval.getNextStartTime(lastTime)
                        }

                        var count = 0
                        var lastChecked = 0L
                        while (currentStartTime < newestTime) {
                            lastChecked = currentStartTime
                            generateAdditionalInterval(connection, destinationInterval, TransactionDataType.TRANSACTION, currentStartTime)
                            generateAdditionalInterval(connection, destinationInterval, TransactionDataType.OVERDUE, currentStartTime)

                            currentStartTime = destinationInterval.getNextStartTime(currentStartTime)
                            count++
                        }
                        if (lastChecked > 0) {
                            for (transactionDataType in TransactionDataType.entries) {
                                deleteOldData(connection, previousInterval, transactionDataType)
                            }
                            writeConsolidationInfo(connection, destinationInterval, lastChecked)
                        }
                        if (count > 0) {
                            generationCount[destinationInterval] = count
                        }
                    }
                }
                VmManagerImpl.SERVER_LOGGER.info("Finished transaction consolidation{}", if (generationCount.isEmpty()) "" else " $generationCount")
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error consolidating transactions", e)
        }
        return now
    }

    private fun getTimeExtremum(function: String, connection: Connection, sourceInterval: TransactionDataInterval): Long {
        var ret = 0L
        connection.prepareStatement("SELECT $function(snapshotTime) FROM " + sourceInterval.getTableName(TransactionDataType.TRANSACTION))
            .use { readStatement ->
                readStatement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        ret = resultSet.getLong(1)
                    }
                }
            }
        return ret
    }

    fun recordTransactionTree(iteration: Long, connectionEntry: CurrentConnectionEntry, vm: VM, snapshotTimeStamp: Long, nanoTime: Long) {
        connectionEntry.agentConnection.executeCommand(CommandType.DATA_SET, DataSetParameter(snapshotTimeStamp), object : Handler<DataSetResult>() {
            override fun handle(result: DataSetResult) {
                databaseWriter.executeInWriter {
                    try {
                        dataSource.connection.use { connection ->
                            storeTransactions(connection, iteration, connectionEntry, vm, snapshotTimeStamp, nanoTime, result)
                        }
                    } catch (e: Exception) {
                        VmManagerImpl.SERVER_LOGGER.error("could not store transaction data for {}", vm.verbose, e)
                    }
                }
            }

            override fun handleThrowable(t: Throwable) {
                VmManagerImpl.CONNECTION_LOGGER.error("transaction tree on {}", vm.verbose, t)
            }
        })
    }

    @MethodTransaction(naming = [Part(text = "store transactions")], group = "storeTransactions")
    fun storeTransactions(
        connection: Connection,
        iteration: Long,
        connectionEntry: CurrentConnectionEntry,
        vm: VM,
        snapshotTimeStamp: Long,
        nanoTime: Long,
        result: DataSetResult
    ) {
        nameManager.getWriteStatements(connection).use { nameWriteStatements ->
            val agentTreeProcessor = AgentTreeProcessor(nameManager, nameWriteStatements) { name ->
                if (transactionCapWarningShown.compareAndSet(false, true)) {
                    collectorContext.addInboxItems(
                        vm.parentIdentifier, "Too many transactions recorded",
                        "The configured transaction cap has been reached. Additional transactions will be grouped under the name:\n\n" +
                                AbstractNameManager.CAPPED_DESCRIPTION + "\n\n" +
                                "The first capped transaction was named:\n\n" +
                                name + "\n\n" +
                                "Please change the transaction naming scheme in the recording config to generated fewer transaction. You can configure the cap in the general settings.",
                        null, vm
                    )
                }
            }
            val processedTransactionTree = agentTreeProcessor.preProcess(result.transactionTree)
            val processedOverdueTree = agentTreeProcessor.preProcess(result.overdueTree)
            handleProcessedTrees(
                connection,
                nameWriteStatements,
                processedTransactionTree,
                processedOverdueTree,
                iteration,
                connectionEntry,
                vm,
                snapshotTimeStamp,
                nanoTime,
                result
            )
        }
    }

    private fun handleProcessedTrees(
        connection: Connection,
        nameWriteStatements: NameWriteStatements,
        processedTransactionTree: ProcessedAgentTree,
        processedOverdueTree: ProcessedAgentTree,
        iteration: Long,
        connectionEntry: CurrentConnectionEntry,
        vm: VM,
        snapshotTimeStamp: Long,
        nanoTime: Long,
        result: DataSetResult
    ) {
        addTelemetryData(nanoTime, snapshotTimeStamp, connectionEntry.vmData, processedTransactionTree, result.realInterval)

        val groupNode = if (vm.type == VmType.POOLED) GROUP_NODE_TYPE_POOLED_VM else GROUP_NODE_TYPE_VM
        Statements.getWriteStatement(connection, TransactionDataInterval.getRecordingInterval(), TransactionDataType.TRANSACTION).use { writeStatement ->
            saveTransactionTree(
                processedTransactionTree,
                StoreVisitor.StoreAgentTreeVisitor(nameManager, nameWriteStatements, writeStatement),
                vm.id,
                groupNode,
                snapshotTimeStamp,
                result.realInterval,
                LongList.of()
            )
        }
        if (processedOverdueTree.childCount > 0) {
            Statements.getWriteStatement(connection, TransactionDataInterval.getRecordingInterval(), TransactionDataType.OVERDUE).use { writeStatement ->
                saveTransactionTree(
                    processedOverdueTree,
                    StoreVisitor.StoreAgentTreeVisitor(nameManager, nameWriteStatements, writeStatement),
                    vm.id,
                    groupNode,
                    snapshotTimeStamp,
                    result.realInterval,
                    LongList.of()
                )
            }
        }

        val groupData = connectionEntry.vmData.parent
        if (groupData != null) {
            groupData.transactionData.addTransactionData(
                iteration,
                vm,
                processedTransactionTree,
                processedOverdueTree,
                snapshotTimeStamp,
                nanoTime,
                result.realInterval,
                this
            )
            groupData.triggerHandler.addTransactions(processedTransactionTree, processedOverdueTree, collectorContext, snapshotTimeStamp, System.nanoTime(), vm)
        }
    }

    fun generateAdditionalIntervals(databaseStartTimes: List<IntervalWithTimestamp>) {
        databaseWriter.executeInWriter {
            try {
                dataSource.connection.use { connection ->
                    generateAdditionalIntervals(connection, databaseStartTimes)
                }
            } catch (e: Exception) {
                VmManagerImpl.SERVER_LOGGER.error("could not generate transaction consolidation", e)
            }
        }
    }

    @MethodTransaction(naming = [Part(text = "generate additional transaction intervals")])
    fun generateAdditionalIntervals(connection: Connection, databaseStartTimes: List<IntervalWithTimestamp>) {
        for (intervalWithTimestamp in databaseStartTimes) {
            val destinationInterval = intervalWithTimestamp.interval
            val previousInterval = destinationInterval.previousInterval ?: continue

            generateAdditionalInterval(connection, destinationInterval, TransactionDataType.TRANSACTION, intervalWithTimestamp.time)
            generateAdditionalInterval(connection, destinationInterval, TransactionDataType.OVERDUE, intervalWithTimestamp.time)

            writeConsolidationInfo(connection, destinationInterval, intervalWithTimestamp.time)
            for (transactionDataType in TransactionDataType.entries) {
                deleteOldData(connection, previousInterval, transactionDataType)
            }

            // delete old day data after a new day interval has been created
            if (destinationInterval == TransactionDataInterval.DAY) {
                for (transactionDataType in TransactionDataType.entries) {
                    deleteOldData(connection, TransactionDataInterval.DAY, transactionDataType)
                }
            }
        }
    }

    private inner class ReadEverythingBlock : CheckedBiConsumer<Connection, LongSet> {
        @MethodTransaction(naming = [Part(text = "mark transaction names")])
        override fun accept(connection: Connection, vmIds: LongSet) {
            val intervals = TransactionDataInterval.entries.toMutableList()
            intervals.reverse()
            for (transactionDataType in listOf(TransactionDataType.TRANSACTION, TransactionDataType.OVERDUE)) {
                for (interval in intervals) {
                    Statements.getReadFullStatements(connection, nameManager, UsedIntervals(interval, null), transactionDataType, true).use { readStatements ->
                        val treeVisitor = object : TreeVisitor() {
                            override fun preVisit(vmId: Long, groupNode: Byte): TransactionTree {
                                vmIds.add(vmId)
                                return TransactionTree()
                            }
                        }
                        visitTransactionTree(treeVisitor, 0, Long.MAX_VALUE, true, readStatements, true)
                    }
                }
            }
        }
    }

    private fun deleteOldData(connection: Connection, sourceInterval: TransactionDataInterval, transactionDataType: TransactionDataType): Long {
        val storageMillis = intervalToStorageMillis[sourceInterval]!!
        val currentTime = System.currentTimeMillis()
        var removedRows = 0
        if (storageMillis.group < Long.MAX_VALUE) {
            @Suppress("SqlWithoutWhere")
            val deleteStatement =
                connection.prepareStatement("delete from " + sourceInterval.getTableName(transactionDataType) + " where snapshotTime<? and groupNode=1")
            deleteStatement.setLong(1, currentTime - storageMillis.group)
            removedRows += deleteStatement.executeUpdate()
        }
        if (storageMillis.vm < Long.MAX_VALUE) {
            @Suppress("SqlWithoutWhere")
            val deleteStatement =
                connection.prepareStatement("delete from " + sourceInterval.getTableName(transactionDataType) + " where snapshotTime<? and groupNode=0")
            deleteStatement.setLong(1, currentTime - storageMillis.vm)
            removedRows += deleteStatement.executeUpdate()
        }
        if (storageMillis.pooled < Long.MAX_VALUE) {
            @Suppress("SqlWithoutWhere")
            val deleteStatement =
                connection.prepareStatement("delete from " + sourceInterval.getTableName(transactionDataType) + " where snapshotTime<? and groupNode=2")
            deleteStatement.setLong(1, currentTime - storageMillis.pooled)
            removedRows += deleteStatement.executeUpdate()
        }
        return removedRows.toLong()
    }

    private fun generateAdditionalInterval(
        connection: Connection,
        destinationInterval: TransactionDataInterval,
        transactionDataType: TransactionDataType,
        startTime: Long
    ) {
        val sourceInterval = destinationInterval.previousInterval ?: return
        Statements.getWriteStatement(connection, destinationInterval, transactionDataType).use { writeStatement ->
            nameManager.getWriteStatements(connection).use { nameWriteStatements ->
                Statements.getReadFullStatements(connection, nameManager, UsedIntervals(sourceInterval, null), transactionDataType, true)
                    .use { readStatements ->
                        val treeVisitor = object : TreeVisitor() {
                            private var currentTree: TransactionTree? = null
                            private val containedIds: LongSet = LongOpenHashSet()

                            override fun preVisit(vmId: Long, groupNode: Byte): TransactionTree {
                                percentage = Percentage()
                                currentTree = TransactionTree()
                                return currentTree!!
                            }

                            override fun preVisitContained(vmId: Long, groupNode: Byte): LongSet {
                                containedIds.clear()
                                return containedIds
                            }

                            override fun postVisit(vmId: Long, groupNode: Byte) {
                                saveTransactionTree(
                                    currentTree!!,
                                    StoreVisitor.StoreTransactionTreeVisitor(nameManager, nameWriteStatements, writeStatement),
                                    vmId,
                                    groupNode,
                                    startTime,
                                    percentage!!.totalTime.toInt(),
                                    containedIds
                                )
                            }
                        }
                        visitTransactionTree(treeVisitor, startTime, startTime + destinationInterval.millis, false, readStatements, false)
                    }
            }
        }
    }

    private fun writeConsolidationInfo(connection: Connection, destinationInterval: TransactionDataInterval, startTime: Long) {
        @Suppress("SqlSourceToSinkFlow")
        val consolidationInfoStatement = connection.prepareStatement(DatabaseHelper.getMergeInto("transaction_consolidation", "type", "consolidationTime"))
        consolidationInfoStatement.setString(1, destinationInterval.name)
        consolidationInfoStatement.setLong(2, startTime)
        consolidationInfoStatement.execute()
        consolidationInfoStatement.close()
    }

    fun saveGroupTree(
        transactionTree: TransactionTree,
        overdueTree: TransactionTree?,
        vm: VM,
        snapshotTime: Long,
        realInterval: Int,
        transactionVms: LongOpenHashSet,
        overdueVms: LongSet
    ) {
        try {
            dataSource.connection.use { connection ->
                nameManager.getWriteStatements(connection).use { nameWriteStatements ->
                    Statements.getWriteStatement(connection, TransactionDataInterval.getRecordingInterval(), TransactionDataType.TRANSACTION)
                        .use { writeStatement ->
                            saveTransactionTree(
                                transactionTree,
                                StoreVisitor.StoreTransactionTreeVisitor(nameManager, nameWriteStatements, writeStatement),
                                vm.id,
                                GROUP_NODE_TYPE_GROUP,
                                snapshotTime,
                                realInterval,
                                transactionVms
                            )
                        }

                    if (overdueTree != null && overdueTree.childCount > 0) {
                        Statements.getWriteStatement(connection, TransactionDataInterval.getRecordingInterval(), TransactionDataType.OVERDUE)
                            .use { writeStatement ->
                                saveTransactionTree(
                                    overdueTree,
                                    StoreVisitor.StoreTransactionTreeVisitor(nameManager, nameWriteStatements, writeStatement),
                                    vm.id,
                                    GROUP_NODE_TYPE_GROUP,
                                    snapshotTime,
                                    realInterval,
                                    overdueVms
                                )
                            }
                    }
                }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("could not store group transaction data for {}", vm, e)
        }
    }

    fun getStoredVms(startTime: Long, endTime: Long, interval: TransactionDataInterval): List<StoredVm> {
        val ret = ArrayList<StoredVm>()
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement("SELECT vmId, max(snapshotTime) maxDataTime FROM " + interval.getTableName(TransactionDataType.TRANSACTION) + " WHERE snapshotTime>=? AND snapshotTime<? AND groupNode!=1 GROUP BY vmId")
                    .use { preparedStatement ->
                        preparedStatement.setLong(1, startTime)
                        preparedStatement.setLong(2, endTime)
                        preparedStatement.executeQuery().use { resultSet ->
                            while (resultSet.next()) {
                                ret.add(StoredVm(resultSet.getLong(1), resultSet.getLong(2)))
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("could not get stored vms", e)
        }
        return ret
    }

    private fun readContainedVms(input: DataInput, containedVms: LongCollection?) {
        val containedCount = input.readInt()
        (0 until containedCount).forEach { _ ->
            val id = input.readLong()
            containedVms?.add(id)
        }
    }

    private fun <T : AbstractTransactionTree<*, T>> saveTransactionTree(
        tree: AbstractTransactionTree<*, T>,
        storeVisitor: StoreVisitor<T>,
        vmId: Long,
        groupNode: Byte,
        timestamp: Long,
        realInterval: Int,
        containedVms: LongCollection
    ) {
        writeContainedVms(storeVisitor.out!!, containedVms)
        tree.visit(storeVisitor)

        val transactionStatement = storeVisitor.writeStatement
        writeBlob(
            transactionStatement,
            vmId,
            groupNode,
            timestamp,
            realInterval,
            ByteArrayInputStream(storeVisitor.getBuffer(), 0, storeVisitor.size()),
            storeVisitor.size()
        )
    }

    fun getTimeExtremum(
        aggregateFunction: String,
        startTime: Long,
        endTime: Long,
        dataInterval: TransactionDataInterval,
        transactionDataType: TransactionDataType,
        vm: VM
    ): Long? {
        var ret: Long? = null
        try {
            dataSource.connection.use { connection ->
                @Suppress("SqlSourceToSinkFlow")
                connection.prepareStatement("SELECT $aggregateFunction(snapshotTime) FROM " + dataInterval.getTableName(transactionDataType) + " WHERE snapshotTime>=? AND snapshotTime<=? AND vmId=?")
                    .use { statement ->
                        statement.setLong(1, startTime)
                        statement.setLong(2, endTime)
                        statement.setLong(3, vm.id)
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                val value = resultSet.getLong(1)
                                if (value != 0L) {
                                    ret = value
                                }
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error reading transaction time extremum", e)
        }
        return ret
    }

    fun visitTransactionTreeData(usedVms: Collection<VM>, transactionCursor: TransactionCursorImpl, mergePolicies: Boolean, visitor: TreeVisitor) {
        if (usedVms.isEmpty() || !transactionCursor.availability.isAvailable) {
            return
        }

        try {
            dataSource.connection.use { connection ->
                val dataInterval =
                    TransactionDataInterval.getUsedIntervals(transactionCursor.interval, transactionCursor.vm?.isGroupNode == true, transactionCursor.isLatest)

                Statements.getReadSpecificStatements(connection, nameManager, dataInterval, transactionCursor.transactionDataType).use { readStatements ->
                    for (vm in usedVms) {
                        readStatements.setVmId(vm.id)
                        visitTransactionTree(
                            visitor,
                            transactionCursor.startTime,
                            transactionCursor.startTime + transactionCursor.interval.timeExtent,
                            mergePolicies,
                            readStatements,
                            false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error visiting transaction tree", e)
        }
    }

    private fun visitTransactionTree(
        treeVisitor: TreeVisitor,
        startTime: Long,
        endTime: Long,
        mergePolicies: Boolean,
        readStatements: ReadStatementPair,
        readAll: Boolean
    ) {
        val queryNameValueStatement = readStatements.largeStatements!!.queryNameValueStatement!!
        val dataVisitor = object : DataVisitor() {
            private var transactionTree: TransactionTree? = null
            private var containedVms: LongSet? = null

            override fun preVisit(vmId: Long, groupNode: Byte): Boolean {
                transactionTree = treeVisitor.preVisit(vmId, groupNode)
                containedVms = treeVisitor.preVisitContained(vmId, groupNode)
                return transactionTree != null
            }

            override fun postVisit(vmId: Long, groupNode: Byte, realInterval: Long) {
                treeVisitor.postVisit(vmId, groupNode, realInterval, endTime - startTime)
            }

            override fun read(data: ByteArray, version: Int, addCurrent: Boolean, time: Long, realInterval: Long) {
                val input = DataInputStream(ByteArrayInputStream(data))
                readContainedVms(input, containedVms)
                readTransactionTree(addCurrent, mergePolicies, transactionTree, TransactionTree(), input, version, queryNameValueStatement, true)
            }
        }
        visitData(dataVisitor, startTime, endTime, readStatements, readAll)
    }

    private fun visitData(dataVisitor: DataVisitor, startTime: Long, endTime: Long, statementPair: ReadStatementPair, readAll: Boolean) {
        val largeResult = visitData(dataVisitor, startTime, endTime, statementPair.largeStatements!!, readAll)
        val smallStatements = statementPair.smallStatements
        if (smallStatements != null) {
            if (largeResult.maxSnapshotTime == Long.MIN_VALUE) { // no hour data
                visitData(dataVisitor, startTime, endTime, smallStatements, readAll)
            } else {
                visitData(dataVisitor, startTime, largeResult.minSnapshotTime, smallStatements, readAll)
                visitData(dataVisitor, largeResult.maxSnapshotTime + statementPair.largeStatements!!.interval.millis, endTime, smallStatements, readAll)
            }
        }
    }

    private fun visitData(dataVisitor: DataVisitor, startTime: Long, endTime: Long, readStatements: ReadStatements, readAll: Boolean): VisitResult {
        val visitResult = VisitResult()
        var previousVmId = 0L
        var previousGroupNode: Byte = 0

        var vmRealInterval = 0L
        readStatements.readTransactionStatement!!.setLong(1, startTime)
        readStatements.readTransactionStatement!!.setLong(2, endTime)
        readStatements.readTransactionStatement!!.executeQuery().use { resultSet ->
            var readCurrent = true

            var hasNext = resultSet.next()
            while (hasNext) {
                var data: ByteArray? = null
                val version = resultSet.getInt(2)
                val realInterval = resultSet.getInt(3)
                val currentSnapshotTime = resultSet.getLong(4)
                val vmId = resultSet.getLong(5)
                val groupNode = resultSet.getByte(6)

                visitResult.update(currentSnapshotTime)

                if (previousVmId != vmId) {
                    if (previousVmId != 0L) {
                        dataVisitor.postVisit(previousVmId, previousGroupNode, vmRealInterval)
                    }
                    vmRealInterval = 0
                    readCurrent = dataVisitor.preVisit(vmId, groupNode)
                    previousVmId = vmId
                    previousGroupNode = groupNode
                }

                if (readCurrent) {
                    data = resultSet.getBytes(1)
                }

                hasNext = resultSet.next()

                val addCurrent = !hasNext || resultSet.getLong(5) != vmId
                try {
                    if (readAll || (currentSnapshotTime + readStatements.interval.millis <= endTime)) {
                        vmRealInterval += realInterval
                        if (readCurrent) {
                            dataVisitor.read(data!!, version, addCurrent, currentSnapshotTime, realInterval.toLong())
                        }
                    }
                } catch (e: Exception) {
                    VmManagerImpl.SERVER_LOGGER.error("could not load transaction data ", e)
                }
            }
        }

        if (previousVmId != 0L) {
            dataVisitor.postVisit(previousVmId, previousGroupNode, vmRealInterval)
        }
        return visitResult
    }

    private fun readTransactionTree(
        addCurrent: Boolean,
        mergePolicies: Boolean,
        parent: TransactionTree?,
        lookupTree: TransactionTree,
        input: DataInputStream,
        version: Int,
        queryNameStatement: PreparedStatement,
        root: Boolean
    ): TransactionTree? {
        val transactionName: String?
        val transactionType: TransactionType?
        val nameId: Long
        transactionType = TransactionType.fromId(input.readShort().toInt())
        nameId = readId(input)
        transactionName = nameManager.getName(nameId, queryNameStatement)
        val type = readType(input, queryNameStatement)
        val currentTree: TransactionTree? = if (root) {
            parent
        } else {
            @Suppress("StringEquality")
            if ((type === PolicyType.PARTIAL.typeString && !addCurrent) || parent == null) {
                null
            } else {
                parent.getOrCreateChild(lookupTree.init(nameId, transactionName, transactionType, if (mergePolicies) PolicyType.NORMAL.typeString else type))
            }
        }
        val time = input.readLong()
        val count = input.readLong()
        if (currentTree != null) {
            currentTree.nameId = nameId
            currentTree.addTime(time)
            currentTree.addCount(count)
        }

        val childrenCount = input.readInt()
        (0 until childrenCount).forEach { _ ->
            readTransactionTree(addCurrent, mergePolicies, currentTree, lookupTree, input, version, queryNameStatement, false)
        }

        return currentTree
    }

    private fun readType(input: DataInputStream, queryNameStatement: PreparedStatement): String {
        val typeId = readId(input)
        val specialPolicyType = PolicyType.getSpecialById(typeId)
        return if (specialPolicyType != null) {
            specialPolicyType.typeString
        } else {
            nameManager.getName(typeId, queryNameStatement)
        }
    }

    fun deleteVMs(connection: Connection, vms: List<VM>) {
        for (interval in TransactionDataInterval.entries) {
            for (transactionDataType in TransactionDataType.entries) {
                @Suppress("SqlWithoutWhere")
                val preparedStatement = connection.prepareStatement("DELETE FROM " + interval.getTableName(transactionDataType) + " WHERE vmId = ?")
                for (vm in vms) {
                    preparedStatement.setLong(1, vm.id)
                    preparedStatement.execute()
                }
                preparedStatement.close()
            }
        }
    }

    fun <T, U : AbstractTransactionTree<T, U>> addTelemetryData(
        nanoTime: Long,
        snapshotTimeStamp: Long,
        vmData: AbstractVmData,
        transactionTree: AbstractTransactionTree<T, U>,
        realInterval: Int
    ) {
        val countVisitor = CountVisitor<T, U>()
        if (realInterval > 0) {
            transactionTree.visit(countVisitor)
        }
        addCount(nanoTime, snapshotTimeStamp, vmData, countVisitor.totalCount, Telemetry.TRANSACTIONS.mainId, realInterval)
    }

    private fun addCount(nanoTime: Long, snapshotTimeStamp: Long, vmData: AbstractVmData, count: CountVisitor.Count, mainId: String, realInterval: Int) {
        val perSecondAndScaleMultiplier = getTransactionTelemetryMultiplier(realInterval.toLong())
        addTelemetryValue(nanoTime, snapshotTimeStamp, vmData, mainId, TelemetryType.SUB_ID_NORMAL, count.normal * perSecondAndScaleMultiplier)
        addTelemetryValue(nanoTime, snapshotTimeStamp, vmData, mainId, TelemetryType.SUB_ID_SLOW, count.slow * perSecondAndScaleMultiplier)
        addTelemetryValue(nanoTime, snapshotTimeStamp, vmData, mainId, TelemetryType.SUB_ID_VERY_SLOW, count.verySlow * perSecondAndScaleMultiplier)
        addTelemetryValue(nanoTime, snapshotTimeStamp, vmData, mainId, TelemetryType.SUB_ID_ERROR, count.error * perSecondAndScaleMultiplier)
        addTelemetryValue(nanoTime, snapshotTimeStamp, vmData, mainId, TelemetryType.SUB_ID_COMPLETED, count.completed * perSecondAndScaleMultiplier)
        addTelemetryValue(nanoTime, snapshotTimeStamp, vmData, mainId, TelemetryType.SUB_ID_AVERAGE, count.getAverageTime(false))
    }

    private fun addTelemetryValue(nanoTime: Long, snapshotTimeStamp: Long, vmData: AbstractVmData, mainId: String, subId: String, value: Long) {
        val identifier = TelemetryIdentifier(mainId, subId)
        vmData.addTelemetryData(
            nanoTime, snapshotTimeStamp, identifier, value,
            addToParent = false,
            groupAveraged = false,
            collectionType = collectionTypeResolver.getCollectionType(identifier)
        )
    }

    abstract class TreeVisitor {
        var percentage: Percentage? = null
            protected set

        abstract fun preVisit(vmId: Long, groupNode: Byte): TransactionTree

        open fun preVisitContained(vmId: Long, groupNode: Byte): LongSet? = null

        protected open fun postVisit(vmId: Long, groupNode: Byte) {
        }

        fun postVisit(vmId: Long, groupNode: Byte, realInterval: Long, expectedInterval: Long) {
            if (percentage == null) {
                percentage = Percentage()
            }
            percentage!!.addPart(realInterval, expectedInterval)
            postVisit(vmId, groupNode)
        }
    }

    private abstract class DataVisitor {
        open fun preVisit(vmId: Long, groupNode: Byte): Boolean = true

        abstract fun postVisit(vmId: Long, groupNode: Byte, realInterval: Long)

        abstract fun read(data: ByteArray, version: Int, addCurrent: Boolean, time: Long, realInterval: Long)
    }

    class StoredVm(val vmId: Long, val newestDataTime: Long)

    private class StorageMillis(val group: Long, val vm: Long, val pooled: Long)

    private class VisitResult {
        var maxSnapshotTime = Long.MIN_VALUE
        var minSnapshotTime = Long.MAX_VALUE

        fun update(currentSnapshotTime: Long) {
            maxSnapshotTime = maxOf(maxSnapshotTime, currentSnapshotTime)
            minSnapshotTime = minOf(minSnapshotTime, currentSnapshotTime)
        }
    }

    companion object {
        private const val GROUP_NODE_TYPE_VM: Byte = 0
        private const val GROUP_NODE_TYPE_GROUP: Byte = 1
        private const val GROUP_NODE_TYPE_POOLED_VM: Byte = 2

        fun getTransactionTelemetryMultiplier(realInterval: Long): Long =
            if (realInterval == 0L) 0 else 1000 * 10000 / realInterval

        private fun writeBlob(
            writeStatement: PreparedStatement,
            vmId: Long,
            groupNode: Byte,
            timestamp: Long,
            realInterval: Int,
            inputStream: InputStream,
            length: Int
        ) {
            writeStatement.setLong(1, vmId)
            writeStatement.setLong(2, timestamp)
            writeStatement.setByte(3, groupNode)
            writeStatement.setLong(4, realInterval.toLong())
            writeStatement.setBinaryStream(5, inputStream, length)
            writeStatement.setInt(6, JvmGuardCommunication.PROTOCOL_VERSION)
            writeStatement.execute()
        }

        fun writeContainedVms(out: DataOutput, containedVms: LongCollection?) {
            if (containedVms == null) {
                out.writeInt(0)
            } else {
                out.writeInt(containedVms.size)
                val iterator = containedVms.iterator()
                while (iterator.hasNext()) {
                    out.writeLong(iterator.nextLong())
                }
            }
        }

        fun readId(input: DataInput): Long {
            return if (input.readBoolean()) {
                input.readLong()
            } else {
                input.readInt().toLong()
            }
        }

        fun writeId(out: DataOutput, id: Long) {
            if (id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
                out.writeBoolean(true)
                out.writeLong(id)
            } else {
                out.writeBoolean(false)
                out.writeInt(id.toInt())
            }
        }

        fun getVerboseGroup(vm: VM): String {
            var group = vm.groupName
            group = if (group.isEmpty()) {
                "root group"
            } else if (vm.type == VmType.POOLED) {
                "pool $group"
            } else {
                "group $group"
            }
            return group
        }
    }
}
