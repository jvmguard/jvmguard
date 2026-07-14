package dev.jvmguard.common.helper

import java.sql.Statement

object DatabaseHelper {

    fun getMergeInto(tableName: String, vararg columns: String): String =
        "MERGE INTO " + getInsertIntoContent(tableName, columns)

    fun createTableIfNotExists(statement: Statement, description: String) {
        @Suppress("SqlSourceToSinkFlow")
        statement.execute("CREATE TABLE IF NOT EXISTS $description")
    }

    fun createIndexIfNotExists(statement: Statement, indexName: String, tableName: String, vararg columns: String) {
        @Suppress("SqlSourceToSinkFlow")
        statement.execute(
            "CREATE INDEX IF NOT EXISTS $indexName ON $tableName (" +
                    removeLengthSpecification(columns).joinToString(", ") + ")"
        )
    }

    fun createIndexIfNotExists(statement: Statement, indexName: String, tableName: String, columns: List<String>) {
        createIndexIfNotExists(statement, indexName, tableName, *columns.toTypedArray())
    }

    private fun getInsertIntoContent(tableName: String, columns: Array<out String>): String =
        tableName + " (" + columns.joinToString(", ") + ") values (" + getQuestionMarks(columns.size) + ")"

    private fun getQuestionMarks(count: Int): String =
        (0 until count).joinToString(", ") { "?" }

    private fun removeLengthSpecification(columns: Array<out String>): List<String> =
        columns.map { column ->
            val bracketPos = column.indexOf("(")
            if (bracketPos > -1) column.substring(0, bracketPos) else column
        }
}
