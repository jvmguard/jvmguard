package dev.jvmguard.collector.transactions.util

import dev.jvmguard.common.helper.DatabaseHelper
import dev.jvmguard.data.transactions.TransactionDataInterval
import dev.jvmguard.data.transactions.TransactionDataInterval.UsedIntervals
import dev.jvmguard.data.transactions.TransactionDataType
import java.sql.Connection
import java.sql.PreparedStatement

object Statements {

    private fun getReadSpecificStatements(
        connection: Connection,
        nameManager: AbstractNameManager,
        interval: TransactionDataInterval?,
        transactionDataType: TransactionDataType
    ): ReadStatements? {
        if (interval == null) {
            return null
        }
        return ReadStatements(interval).apply {
            readTransactionStatement = connection.prepareStatement(
                "SELECT content, version, realInterval, snapshotTime, vmId, groupNode FROM " + interval.getTableName(transactionDataType) + " WHERE snapshotTime>=? AND snapshotTime<? AND vmId=? ORDER BY snapshotTime"
            )
            queryNameValueStatement = nameManager.getQueryNameValueStatement(connection)
        }
    }

    private fun getReadFullStatements(
        connection: Connection,
        nameManager: AbstractNameManager,
        interval: TransactionDataInterval?,
        transactionDataType: TransactionDataType,
        includeGroups: Boolean
    ): ReadStatements? {
        if (interval == null) {
            return null
        }
        return ReadStatements(interval).apply {
            readTransactionStatement = connection.prepareStatement(
                "SELECT content, version, realInterval, snapshotTime, vmId, groupNode FROM " +
                        interval.getTableName(transactionDataType) + " WHERE snapshotTime>=? AND snapshotTime<? " + (if (includeGroups) "" else " AND groupNode <> 1 ") + "ORDER BY vmId,snapshotTime"
            )
            queryNameValueStatement = nameManager.getQueryNameValueStatement(connection)
        }
    }

    fun getReadSpecificStatements(
        connection: Connection,
        nameManager: AbstractNameManager,
        usedIntervals: UsedIntervals,
        transactionDataType: TransactionDataType
    ): ReadStatementPair =
        ReadStatementPair().apply {
            largeStatements = getReadSpecificStatements(connection, nameManager, usedIntervals.largeInterval, transactionDataType)
            smallStatements = getReadSpecificStatements(connection, nameManager, usedIntervals.smallInterval, transactionDataType)
        }

    fun getReadFullStatements(
        connection: Connection,
        nameManager: AbstractNameManager,
        usedIntervals: UsedIntervals,
        transactionDataType: TransactionDataType,
        includeGroups: Boolean
    ): ReadStatementPair =
        ReadStatementPair().apply {
            largeStatements = getReadFullStatements(connection, nameManager, usedIntervals.largeInterval, transactionDataType, includeGroups)
            smallStatements = getReadFullStatements(connection, nameManager, usedIntervals.smallInterval, transactionDataType, includeGroups)
        }

    fun getWriteStatement(connection: Connection, interval: TransactionDataInterval, transactionDataType: TransactionDataType): PreparedStatement =
        @Suppress("SqlSourceToSinkFlow")
        connection.prepareStatement(
            DatabaseHelper.getMergeInto(
                interval.getTableName(transactionDataType),
                "vmId",
                "snapshotTime",
                "groupNode",
                "realInterval",
                "content",
                "version"
            )
        )

    class ReadStatements(var interval: TransactionDataInterval) : AutoCloseable {
        var queryNameValueStatement: PreparedStatement? = null
        var readTransactionStatement: PreparedStatement? = null

        override fun close() {
            queryNameValueStatement?.close()
            readTransactionStatement?.close()
        }
    }

    class ReadStatementPair : AutoCloseable {
        var largeStatements: ReadStatements? = null
        var smallStatements: ReadStatements? = null

        override fun close() {
            largeStatements?.close()
            smallStatements?.close()
        }

        fun setVmId(id: Long) {
            largeStatements!!.readTransactionStatement!!.setLong(3, id)
            smallStatements?.readTransactionStatement?.setLong(3, id)
        }
    }
}
