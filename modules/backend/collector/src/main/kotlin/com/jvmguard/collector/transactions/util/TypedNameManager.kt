package com.jvmguard.collector.transactions.util

import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.common.helper.DatabaseHelper
import it.unimi.dsi.fastutil.longs.LongSet
import org.jooq.lambda.fi.util.function.CheckedBiConsumer
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement

class TypedNameManager(
    connection: Connection,
    tablePrefix: String,
    readEverythingBlock: CheckedBiConsumer<Connection, LongSet>?,
    properties: JvmGuardProperties,
) : AbstractNameManager(connection, tablePrefix, readEverythingBlock, properties) {

    private val typeToCount = IntArray(NameType.entries.size)
    private val typeToStartCount = IntArray(typeToCount.size)
    private val typeToCap = IntArray(typeToCount.size) { Integer.MAX_VALUE }

    init {
        connection.createStatement().use { statement ->
            DatabaseHelper.createTableIfNotExists(statement, tablePrefix + "_caps(type INT PRIMARY KEY, startCount BIGINT)")

            @Suppress("SqlSourceToSinkFlow")
            statement.executeQuery("select type, count(id) from $tableName GROUP BY type").use { resultSet ->
                while (resultSet.next()) {
                    val type = resultSet.getInt(1)
                    if (type in typeToCount.indices) {
                        typeToCount[type] = resultSet.getInt(2)
                    }
                }
            }
            @Suppress("SqlSourceToSinkFlow")
            statement.executeQuery("select type, startCount from " + tablePrefix + "_caps").use { resultSet ->
                while (resultSet.next()) {
                    val type = resultSet.getInt(1)
                    if (type in typeToCount.indices) {
                        typeToStartCount[type] = resultSet.getInt(2)
                    }
                }
            }
        }
    }

    override val isLongId: Boolean
        get() = true

    @Synchronized
    fun setCap(type: NameType, cap: Int) {
        typeToCap[type.ordinal] = cap
    }

    @Synchronized
    fun resetCapCount(type: NameType, ifCappedOnly: Boolean, connection: Connection) {
        if (!ifCappedOnly || isCapped(type.ordinal)) {
            updateStartCount(connection, type.ordinal)
        }
    }

    private fun updateStartCount(connection: Connection, type: Int) {
        typeToStartCount[type] = typeToCount[type]
        @Suppress("SqlSourceToSinkFlow")
        connection.prepareStatement(DatabaseHelper.getMergeInto(tablePrefix + "_caps", "type", "startCount")).use { statement ->
            statement.setInt(1, type)
            statement.setInt(2, typeToStartCount[type])
            statement.execute()
        }
    }

    @Synchronized
    override fun getNameId(str: String?, type: NameType, nameWriteStatements: NameWriteStatements): Long =
        getNameId(str, nameWriteStatements, type.ordinal, false)

    @Synchronized
    fun getCappedNameId(str: String?, type: NameType, nameWriteStatements: NameWriteStatements): Long =
        getNameId(str, nameWriteStatements, type.ordinal, true)

    fun isCapped(type: NameType): Boolean = isCapped(type.ordinal)

    @Synchronized
    override fun isCapped(type: Int): Boolean =
        typeToCount[type] - typeToStartCount[type] >= typeToCap[type]

    override fun fillParameters(statement: PreparedStatement, str: String, type: Int) {
        statement.setInt(1, type)
        statement.setString(2, str)
    }

    override fun deleteUnmarked(connection: Connection): Int {
        logUnmarked(connection)

        var collectedTotal = 0
        @Suppress("SqlSourceToSinkFlow")
        connection.prepareStatement("delete from $tableName where mark=false and type=?").use { deleteStatement ->
            for (type in NameType.entries.indices) {
                deleteStatement.setInt(1, type)
                val collectedCount = deleteStatement.executeUpdate()
                synchronized(this) {
                    typeToCount[type] -= collectedCount
                    if (typeToCount[type] < 0) {
                        typeToCount[type] = 0
                    }
                    if (typeToCount[type] < typeToStartCount[type]) {
                        updateStartCount(connection, type)
                    }
                }
                collectedTotal += collectedCount
            }
        }
        return collectedTotal
    }

    override fun addCount(type: Int) {
        typeToCount[type]++
    }

    override val columns: String
        get() = super.columns + ", type TINYINT NOT NULL DEFAULT 0"

    override val indexColumns: List<String>
        get() = buildList {
            add("type")
            addAll(super.indexColumns)
        }

    override fun getWriteStatements(connection: Connection): NameWriteStatements =
        @Suppress("SqlSourceToSinkFlow")
        NameWriteStatements(
            connection.prepareStatement("SELECT id FROM $tableName WHERE type=? and \"value\"=? ORDER BY id"),
            connection.prepareStatement("INSERT INTO $tableName(type, \"value\") VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)
        )

    companion object {
        const val CAPPED_DESCRIPTION = AbstractNameManager.CAPPED_DESCRIPTION
    }
}
