package dev.jvmguard.database

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class H2SchemaCompatibilityTest {

    @Test
    fun migrationAppliesOnH2_2x() {
        memoryDatabase("compat").use { connection ->
            runScript(connection, "/db/migration/V1__initial_schema.sql")
            assertTableExists(connection, "CONFIG_STORAGE")
            assertTableExists(connection, "VM")
            assertTableExists(connection, "VM_INSTANCE")
            assertTableExists(connection, "ADDITIONAL_TELEMETRY_FORMAT")
            assertTableExists(connection, "TELEMETRY_LIST_IDS")
            assertTableExists(connection, "SNAPSHOT_FILE")
            assertTableExists(connection, "INBOX")
            assertTableExists(connection, "TRANSACTION_CONSOLIDATION")
        }
    }

    @Test
    fun configStorageRoundTrips() {
        memoryDatabase("compat_config").use { connection ->
            runScript(connection, "/db/migration/V1__initial_schema.sql")
            val id: Long
            connection.prepareStatement(
                "INSERT INTO config_storage (bean_type, content) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS,
            ).use { insert ->
                insert.setString(1, "user")
                insert.setString(2, "<java><string>x</string></java>")
                insert.executeUpdate()
                insert.generatedKeys.use { keys ->
                    assertTrue(keys.next())
                    id = keys.getLong(1)
                }
            }
            connection.prepareStatement("SELECT content FROM config_storage WHERE bean_type=? AND id=?").use { select ->
                select.setString(1, "user")
                select.setLong(2, id)
                select.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next())
                    assertEquals("<java><string>x</string></java>", resultSet.getString("content"))
                }
            }
        }
    }

    @Test
    fun nameInterningQuotedValueRoundTrips() {
        memoryDatabase("compat_names").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE IF NOT EXISTS vm_names($NAME_TABLE_COLUMNS)")
                statement.execute("CREATE INDEX IF NOT EXISTS vm_index ON vm_names (\"value\")")
            }
            connection.prepareStatement(
                "INSERT INTO vm_names(\"value\") VALUES(?)", Statement.RETURN_GENERATED_KEYS,
            ).use { insert ->
                insert.setString(1, "com.example.Demo")
                insert.executeUpdate()
            }
            connection.prepareStatement("SELECT id FROM vm_names WHERE \"value\"=? ORDER BY id").use { query ->
                query.setString(1, "com.example.Demo")
                query.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next())
                    val id = resultSet.getLong(1)
                    connection.prepareStatement("SELECT \"value\" FROM vm_names WHERE id=?").use { reverse ->
                        reverse.setLong(1, id)
                        reverse.executeQuery().use { reverseResult ->
                            assertTrue(reverseResult.next())
                            assertEquals("com.example.Demo", reverseResult.getString(1))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun mediumBlobMergeAndIntervalTablesWork() {
        memoryDatabase("compat_blob").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    "CREATE TABLE telemetry_data_10 (vmId BIGINT NOT NULL, dataTime BIGINT NOT NULL, " +
                            "listId INT NOT NULL, version INT NOT NULL, content MEDIUMBLOB NOT NULL, PRIMARY KEY(dataTime, vmId))",
                )
                statement.execute(
                    "CREATE TABLE transaction_minute (vmId BIGINT NOT NULL, snapshotTime BIGINT NOT NULL, " +
                            "groupNode TINYINT NOT NULL, realInterval INT, version INT, content LONGBLOB, PRIMARY KEY(snapshotTime, vmId))",
                )
                statement.execute("CREATE TABLE transaction_caps(type INT PRIMARY KEY, startCount BIGINT)")
                // The H2 MERGE upsert form DatabaseHelper.getMergeInto() produces.
                connection.prepareStatement(
                    "MERGE INTO telemetry_data_10 (vmId, dataTime, listId, version, content) values (?, ?, ?, ?, ?)",
                ).use { merge ->
                    merge.setLong(1, 1)
                    merge.setLong(2, 1000)
                    merge.setInt(3, 1)
                    merge.setInt(4, 1)
                    merge.setBytes(5, byteArrayOf(1, 2, 3))
                    merge.executeUpdate()
                    merge.setBytes(5, byteArrayOf(4, 5, 6))
                    merge.executeUpdate()
                }
                statement.executeQuery("SELECT COUNT(*) FROM telemetry_data_10").use { resultSet ->
                    assertTrue(resultSet.next())
                    assertEquals(1, resultSet.getInt(1), "MERGE on the same PK must upsert, not duplicate")
                }
            }
        }
    }

    @Test
    fun v2IndexesApplyAndAreIdempotent() {
        memoryDatabase("compat_v2").use { connection ->
            runScript(connection, "/db/migration/V1__initial_schema.sql")
            runScript(connection, "/db/migration/V2__snapshot_and_vm_indexes.sql")
            assertIndexExists(connection, "VM", "VM_QUERY")
            assertIndexExists(connection, "SNAPSHOT_FILE", "SNAPSHOT_FILE_QUERY")
            assertIndexExists(connection, "SNAPSHOT_FILE", "SNAPSHOT_FILE_TYPE")
            runScript(connection, "/db/migration/V2__snapshot_and_vm_indexes.sql")
        }
    }

    private fun assertIndexExists(connection: Connection, upperCaseTableName: String, upperCaseIndexName: String) {
        connection.metaData.getIndexInfo(null, null, upperCaseTableName, false, false).use { indexes ->
            var found = false
            while (indexes.next()) {
                if (indexes.getString("INDEX_NAME") == upperCaseIndexName) {
                    found = true
                }
            }
            assertTrue(found, "expected index $upperCaseIndexName on $upperCaseTableName after migration")
        }
    }

    private fun assertTableExists(connection: Connection, upperCaseTableName: String) {
        connection.metaData.getTables(null, null, upperCaseTableName, null).use { tables ->
            assertTrue(tables.next(), "expected table $upperCaseTableName to exist after migration")
        }
    }

    private fun runScript(connection: Connection, resourcePath: String) {
        // Strip line comments first, then split on ';' (a comment may itself contain a semicolon).
        val script = stripComments(readResource(resourcePath))
        connection.createStatement().use { statement ->
            for (rawStatement in script.split(";")) {
                val sql = rawStatement.trim()
                if (sql.isNotEmpty()) {
                    statement.execute(sql)
                }
            }
        }
    }

    private fun stripComments(script: String): String = buildString {
        for (line in script.split("\n")) {
            if (!line.trim().startsWith("--")) {
                append(line).append('\n')
            }
        }
    }

    private fun readResource(resourcePath: String): String {
        val stream = H2SchemaCompatibilityTest::class.java.getResourceAsStream(resourcePath)
        assertNotNull(stream, "missing classpath resource $resourcePath")
        return stream!!.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }

    companion object {
        // Mirrors AbstractNameManager.getColumns() / getWriteStatements() / getQueryNameValueStatement().
        // The "value" column MUST be quoted on H2 2.x.
        private const val NAME_TABLE_COLUMNS =
            "id INT AUTO_INCREMENT NOT NULL PRIMARY KEY, \"value\" VARCHAR(10000) NOT NULL, mark BOOL NOT NULL DEFAULT TRUE"

        private fun memoryDatabase(name: String): Connection =
            DriverManager.getConnection("jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1", "sa", "")
    }
}
