package dev.jvmguard.collector.transactions.util

import dev.jvmguard.collector.main.VmManagerImpl
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.common.helper.DatabaseHelper
import it.unimi.dsi.fastutil.longs.*
import org.jooq.lambda.fi.util.function.CheckedBiConsumer
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

@Suppress("SqlResolve")
abstract class AbstractNameManager protected constructor(
    connection: Connection,
    protected val tablePrefix: String,
    private val readEverythingBlock: CheckedBiConsumer<Connection, LongSet>?,
    private val properties: JvmGuardProperties,
) {
    private val nameWriteCache = HashMap<String, Long>()
    private val nameReadCache = Long2ObjectOpenHashMap<String>()

    private val cappedId = if (isLongId) Long.MAX_VALUE else Integer.MAX_VALUE.toLong()

    private var usedGcIds: LongSet? = null
    private var gcThread: Thread? = null
    private var preGcCount: Long = 0

    abstract fun getNameId(str: String?, type: NameType, nameWriteStatements: NameWriteStatements): Long

    init {
        val statement = connection.createStatement()
        DatabaseHelper.createTableIfNotExists(statement, "$tableName($columns)")
        try {
            DatabaseHelper.createIndexIfNotExists(statement, tablePrefix + "_index", tablePrefix + "_names", indexColumns)
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.warn("index for table {} was not created", tableName, e)
        }
        statement.close()
        addNameManager(this)
    }

    protected open val tableName: String
        get() = tablePrefix + "_names"

    protected open val indexColumns: List<String>
        get() {
            val nameIndexLength = properties.nameIndexLength
            return listOf("\"value\"" + (if (nameIndexLength == 0) "" else "($nameIndexLength)"))
        }

    protected open val isLongId: Boolean
        get() = false

    fun isCappedId(nameId: Long): Boolean = nameId == cappedId

    protected open val columns: String
        get() = "id " + (if (isLongId) "BIGINT" else "INT") + " AUTO_INCREMENT NOT NULL PRIMARY KEY, \"value\" VARCHAR($MAX_STRING_SIZE) NOT NULL, mark BOOL NOT NULL DEFAULT TRUE"

    fun startGc(connection: Connection): Boolean {
        if (readEverythingBlock != null) {
            synchronized(this) {
                usedGcIds = LongOpenHashSet()
                gcThread = Thread.currentThread()
            }
            preGcCount = 0
            @Suppress("SqlSourceToSinkFlow")
            connection.prepareStatement("select count(id) from $tableName").use { countStatement ->
                countStatement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        preGcCount = resultSet.getLong(1)
                    }
                }
            }
            @Suppress("SqlSourceToSinkFlow", "SqlWithoutWhere")
            connection.prepareStatement("update $tableName set mark=false").use { removeMarkStatement ->
                removeMarkStatement.execute()
            }
            return true
        }
        return false
    }

    fun markUsedRows(connection: Connection, usedVmIds: LongSet) {
        if (readEverythingBlock != null) {
            val autoCommit = connection.autoCommit
            try {
                connection.autoCommit = false
                readEverythingBlock.accept(connection, usedVmIds)
            } finally {
                try {
                    connection.commit()
                } finally {
                    connection.autoCommit = autoCommit
                }
            }
        }
    }

    fun sweepUnusedRows(connection: Connection): Long {
        if (readEverythingBlock != null) {
            // all newly created entries will have set the mark field to true by default so won't be deleted if created after this block
            val usedGcIds = synchronized(this) {
                val ids = this.usedGcIds
                this.usedGcIds = null
                gcThread = null
                ids
            }
            writeMark(connection, usedGcIds!!)
            val collectedCount = deleteUnmarked(connection).toLong()
            if (collectedCount > 0) {
                synchronized(this) {
                    nameWriteCache.clear()
                }
                var percentage = 0.0
                if (preGcCount > 0) {
                    percentage = 100.0 * collectedCount / preGcCount
                    if (percentage > 100) {
                        percentage = 100.0
                    }
                }
                VmManagerImpl.SERVER_LOGGER.info(String.format("collected %d rows (%.1f %%) from %s", collectedCount, percentage, tableName))
            }
            return collectedCount
        }
        return 0
    }

    @Synchronized
    fun finishGc() {
        usedGcIds = null
        gcThread = null
    }

    protected open fun deleteUnmarked(connection: Connection): Int {
        logUnmarked(connection)
        @Suppress("SqlSourceToSinkFlow")
        connection.prepareStatement("delete from $tableName where mark=false").use { deleteStatement ->
            return deleteStatement.executeUpdate()
        }
    }

    protected fun logUnmarked(connection: Connection) {
        if (properties.isGcDebug) {
            @Suppress("SqlSourceToSinkFlow")
            connection.prepareStatement("select \"value\" from $tableName where mark=false").use { debugStatement ->
                debugStatement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        VmManagerImpl.SERVER_LOGGER.info("collecting {}", resultSet.getString(1))
                    }
                }
            }
        }
    }

    private fun addUsedGcId(id: Long, connection: Connection?) {
        var size = -1
        synchronized(this) {
            val usedGcIds = this.usedGcIds
            if (usedGcIds != null) {
                usedGcIds.add(id)
                size = usedGcIds.size
            }
        }
        if (connection != null && size > properties.maxMemGcIds) {
            try {
                val copiedIds: LongCollection
                synchronized(this) {
                    val usedGcIds = this.usedGcIds
                    if (usedGcIds != null) {
                        copiedIds = LongArrayList(usedGcIds)
                        usedGcIds.clear()
                    } else {
                        copiedIds = LongArrayList()
                    }
                }
                writeMark(connection, copiedIds)
            } catch (t: Throwable) {
                VmManagerImpl.SERVER_LOGGER.error("error during gc of {}", tableName, t)
            }
        }
    }

    private fun setId(preparedStatement: PreparedStatement, index: Int, value: Long) {
        if (isLongId) {
            preparedStatement.setLong(index, value)
        } else {
            preparedStatement.setInt(index, value.toInt())
        }
    }

    private fun getId(resultSet: ResultSet, index: Int): Long =
        if (isLongId) {
            resultSet.getLong(index)
        } else {
            resultSet.getInt(index).toLong()
        }

    private fun writeMark(connection: Connection, usedGcIds: LongCollection) {
        @Suppress("SqlSourceToSinkFlow")
        connection.prepareStatement("update $tableName set mark=true where id in (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")
            .use { setMarkStatement ->
                var parameterIndex = 1
                val iterator = usedGcIds.iterator()
                while (iterator.hasNext()) {
                    val id = iterator.nextLong()
                    setId(setMarkStatement, parameterIndex++, id)
                    if (parameterIndex > MARK_BATCH_SIZE) {
                        setMarkStatement.execute()
                        parameterIndex = 1
                    }
                }
                for (i in parameterIndex..MARK_BATCH_SIZE) {
                    setId(setMarkStatement, i, -1)
                }
                setMarkStatement.execute()
            }
    }

    fun getName(id: Long, queryStatement: PreparedStatement): String {
        if (id == cappedId) {
            return CAPPED_DESCRIPTION
        }
        val gcThread = this.gcThread
        if (gcThread != null && gcThread === Thread.currentThread()) {
            addUsedGcId(id, queryStatement.connection)
        }
        return getNameWithoutGc(id, queryStatement)
    }

    @Synchronized
    private fun getNameWithoutGc(id: Long, queryStatement: PreparedStatement): String {
        if (nameReadCache.size > properties.nameCacheSize) {
            nameReadCache.clear()
        }

        val cachedValue = nameReadCache.get(id)
        if (cachedValue != null) {
            return cachedValue
        }
        setId(queryStatement, 1, id)
        val result: String
        queryStatement.executeQuery().use { resultSet ->
            result = if (resultSet.next()) resultSet.getString(1) else UNKNOWN
        }
        nameReadCache.put(id, result)
        return result
    }

    @Synchronized
    protected fun getNameId(str: String?, nameWriteStatements: NameWriteStatements, type: Int, capped: Boolean): Long {
        val id = getNameIdWithoutGc(str, nameWriteStatements, type, capped, false)
        addUsedGcId(id, null)
        return id
    }

    @Synchronized
    protected fun getNameIdWithoutGc(str: String?, nameWriteStatements: NameWriteStatements, type: Int, capped: Boolean, existingOnly: Boolean): Long {
        @Suppress("StringEquality", "StringReferentialEquality")
        if (str === CAPPED_DESCRIPTION) {
            return cappedId
        }
        var value = str
        if (value == null) {
            value = "<null>"
        } else if (value.length > MAX_STRING_SIZE - 1) {
            value = value.substring(0, MAX_STRING_SIZE - 1)
        }
        if (nameWriteCache.size > properties.nameCacheSize) {
            nameWriteCache.clear()
        }

        val cachedValue = nameWriteCache[value]
        if (cachedValue != null) {
            return cachedValue
        }
        var id = 0L
        fillParameters(nameWriteStatements.queryNameIdStatement, value, type)
        nameWriteStatements.queryNameIdStatement.executeQuery().use { resultSet ->
            if (resultSet.next()) {
                id = getId(resultSet, 1)
            } else if (capped && isCapped(type)) {
                return cappedId
            } else if (existingOnly) {
                return 0
            } else {
                val connection = nameWriteStatements.insertNameStatement.connection
                val autoCommit = connection.autoCommit
                connection.autoCommit = false
                try {
                    fillParameters(nameWriteStatements.insertNameStatement, value, type)
                    nameWriteStatements.insertNameStatement.execute()
                    nameWriteStatements.insertNameStatement.generatedKeys.use { keySet ->
                        if (keySet.next()) {
                            id = getId(keySet, 1)
                        }
                    }
                    connection.commit()
                } finally {
                    connection.autoCommit = autoCommit
                }
                addCount(type)
            }
            nameWriteCache[value] = id
        }
        return id
    }

    protected open fun isCapped(type: Int): Boolean = false

    protected open fun fillParameters(statement: PreparedStatement, str: String, type: Int) {
        statement.setString(1, str)
    }

    protected open fun addCount(type: Int) {
    }

    open fun getWriteStatements(connection: Connection): NameWriteStatements =
        @Suppress("SqlSourceToSinkFlow")
        NameWriteStatements(
            connection.prepareStatement("SELECT id FROM $tableName WHERE \"value\"=? ORDER BY id"),
            connection.prepareStatement("INSERT INTO $tableName(\"value\") VALUES(?)", Statement.RETURN_GENERATED_KEYS)
        )

    fun getQueryNameValueStatement(connection: Connection): PreparedStatement =
        @Suppress("SqlSourceToSinkFlow")
        connection.prepareStatement("SELECT \"value\" FROM $tableName WHERE id=?")

    class NameWriteStatements(
        val queryNameIdStatement: PreparedStatement,
        val insertNameStatement: PreparedStatement,
    ) : AutoCloseable {
        override fun close() {
            queryNameIdStatement.close()
            insertNameStatement.close()
        }
    }

    companion object {
        private const val MAX_STRING_SIZE = 10000
        private const val MARK_BATCH_SIZE = 50

        const val UNKNOWN = "<unknown>"
        const val CAPPED_DESCRIPTION = "<capped description>"

        private val nameManagers = ArrayList<AbstractNameManager>()

        private fun addNameManager(namesManager: AbstractNameManager) {
            synchronized(nameManagers) {
                nameManagers.add(namesManager)
            }
        }

        fun getNameManagers(): List<AbstractNameManager> =
            synchronized(nameManagers) {
                ArrayList(nameManagers)
            }
    }
}
