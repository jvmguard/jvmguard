package com.jvmguard.collector.transactions.util

import com.jvmguard.common.JvmGuardProperties
import it.unimi.dsi.fastutil.longs.LongSet
import org.jooq.lambda.fi.util.function.CheckedBiConsumer
import java.sql.Connection

class UntypedNameManager(
    connection: Connection,
    tablePrefix: String,
    readEverythingBlock: CheckedBiConsumer<Connection, LongSet>?,
    properties: JvmGuardProperties,
) : AbstractNameManager(connection, tablePrefix, readEverythingBlock, properties) {

    override fun getNameId(str: String?, type: NameType, nameWriteStatements: NameWriteStatements): Long =
        getNameId(str, nameWriteStatements, -1, false)

    @Synchronized
    fun getNameId(str: String?, nameWriteStatements: NameWriteStatements): Int =
        getNameId(str, nameWriteStatements, -1, false).toInt()
}
