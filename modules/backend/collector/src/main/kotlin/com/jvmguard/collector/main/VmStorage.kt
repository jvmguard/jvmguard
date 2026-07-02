package com.jvmguard.collector.main

import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.util.collection.ArrayStack
import com.jvmguard.collector.transactions.util.UntypedNameManager
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.common.helper.DatabaseHelper
import com.jvmguard.data.vmdata.VM
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import jakarta.annotation.PostConstruct
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource

@Component
@DependsOnDatabaseInitialization
class VmStorage(
    private val properties: JvmGuardProperties,
    private val dataSource: DataSource,
) {
    private val vmIdCache = Long2ObjectOpenHashMap<VM>()

    private lateinit var nameManager: UntypedNameManager
    private var rootGroup: VM? = null

    @PostConstruct
    fun postConstruct() {
        try {
            dataSource.connection.use { connection ->
                nameManager = UntypedNameManager(connection, "vm", null, properties)

                connection.createStatement().use { statement ->
                    DatabaseHelper.createTableIfNotExists(
                        statement,
                        "vm (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, vmType TINYINT NOT NULL, nameId INT NOT NULL, groupNameId INT NOT NULL, instanceId BIGINT NOT NULL)"
                    )
                    DatabaseHelper.createIndexIfNotExists(statement, "vm_query", "vm", "groupNameId", "vmType", "nameId")

                    DatabaseHelper.createTableIfNotExists(
                        statement,
                        "vm_instance (instanceId BIGINT NOT NULL PRIMARY KEY, vmId BIGINT NOT NULL, hostNameId INT NOT NULL, port INT NOT NULL)"
                    )
                }
                initRootGroup(connection)
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error creating vm table", e)
        }
    }

    private fun initRootGroup(connection: Connection) {
        val emptyNameId: Int
        nameManager.getWriteStatements(connection).use { nameWriteStatements ->
            emptyNameId = nameManager.getNameId("", nameWriteStatements)
        }

        connection.prepareStatement("select id from vm where vmType=? and groupNameId=? and nameId=?").use { statement ->
            statement.setInt(1, VmType.GROUP.databaseId)
            statement.setInt(2, emptyNameId)
            statement.setInt(3, emptyNameId)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    rootGroup = VM(VmType.GROUP, resultSet.getLong("id"), 0, "", "")
                }
            }
        }
        if (rootGroup == null) {
            connection.prepareStatement("insert into vm (vmType, groupNameId, nameId, instanceId) values (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
                .use { statement ->
                    statement.setInt(1, VmType.GROUP.databaseId)
                    statement.setInt(2, emptyNameId)
                    statement.setInt(3, emptyNameId)
                    statement.setLong(4, 0)
                    statement.execute()

                    statement.generatedKeys.use { resultSet ->
                        if (resultSet.next()) {
                            rootGroup = VM(VmType.GROUP, resultSet.getLong(1), 0, "", "")
                        }
                    }
                }
        }
    }

    fun getVmTree(groupVm: VM?, includedType: VmType): Collection<VM> {
        val stack = ArrayStack<VM>()
        val vms = HashSet<VM>()
        when {
            groupVm == null -> {
                stack.push(rootGroup!!)
                vms.add(rootGroup!!)
            }

            groupVm.type.isGroupNode -> {
                stack.push(groupVm)
                vms.add(groupVm)
            }

            else -> {
                vms.add(groupVm)
                return vms
            }
        }

        try {
            dataSource.connection.use { connection ->
                nameManager.getQueryNameValueStatement(connection).use { queryNameValueStatement ->
                    nameManager.getWriteStatements(connection).use { nameWriteStatements ->
                        connection.prepareStatement("select * from vm_instance where vmId=?").use { hostNameStatement ->
                            createVmTreeSelectStatement(connection, includedType).use { statement ->
                                while (!stack.isEmpty()) {
                                    val currentGroup = stack.pop()

                                    statement.setInt(1, nameManager.getNameId(currentGroup.hierarchyPath, nameWriteStatements))
                                    statement.executeQuery().use { resultSet ->
                                        while (resultSet.next()) {
                                            val vm = getVm(resultSet, queryNameValueStatement)
                                            if (vm.isIncluded(currentGroup.qualifiedIdentifier) && vms.add(vm)) {
                                                addHostName(vm, hostNameStatement, queryNameValueStatement)

                                                if (vm.type == VmType.GROUP || (vm.type == VmType.POOL && includedType == VmType.POOLED)) {
                                                    stack.push(vm)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error reading vm tree", e)
        }
        return vms
    }

    private fun createVmTreeSelectStatement(connection: Connection, includedType: VmType): PreparedStatement {
        return if (includedType == VmType.POOLED) {
            connection.prepareStatement("select * from vm where groupNameId=?")
        } else {
            connection.prepareStatement("select * from vm where groupNameId=? and vmType<=?").apply {
                setInt(2, includedType.databaseId)
            }
        }
    }

    private fun addHostName(vm: VM, hostNameStatement: PreparedStatement, queryNameValueStatement: PreparedStatement): VM {
        if (vm.type == VmType.POOLED) {
            hostNameStatement.setLong(1, vm.id)
            hostNameStatement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    vm.port = resultSet.getInt("port")
                    vm.hostName = nameManager.getName(resultSet.getInt("hostNameId").toLong(), queryNameValueStatement)
                }
            }
        }
        return vm
    }

    private fun getVm(resultSet: ResultSet, queryNameValueStatement: PreparedStatement): VM {
        return VM(
            VmType.fromDatabaseId(resultSet.getInt("vmType")),
            resultSet.getLong("id"),
            resultSet.getLong("instanceId"),
            nameManager.getName(resultSet.getInt("nameId").toLong(), queryNameValueStatement),
            nameManager.getName(resultSet.getInt("groupNameId").toLong(), queryNameValueStatement),
        )
    }

    fun getVm(name: String, groupName: String, instanceId: Long, vmType: VmType, hostName: String, port: Int): VM? {
        try {
            dataSource.connection.use { connection ->
                val nameId: Int
                val groupNameId: Int
                val hostNameId: Int
                nameManager.getWriteStatements(connection).use { nameWriteStatements ->
                    nameId = nameManager.getNameId(name, nameWriteStatements)
                    groupNameId = nameManager.getNameId(groupName, nameWriteStatements)
                    hostNameId = nameManager.getNameId(hostName, nameWriteStatements)
                }

                var id: Long = 0
                createVmSelectStatement(connection, vmType, instanceId, nameId, groupNameId).use { statement ->
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            id = resultSet.getLong("id")
                        }
                    }
                }
                if (id == 0L) {
                    connection.prepareStatement("insert into vm (vmType, nameId, groupNameId, instanceId) values(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)
                        .use { statement ->
                            statement.setInt(1, vmType.databaseId)
                            statement.setInt(2, nameId)
                            statement.setInt(3, groupNameId)
                            statement.setLong(4, if (vmType == VmType.POOLED) instanceId else 0)
                            statement.execute()
                            statement.generatedKeys.use { resultSet ->
                                if (resultSet.next()) {
                                    id = resultSet.getLong(1)
                                }
                            }
                        }
                }
                if (!vmType.isGroupNode) {
                    @Suppress("SqlSourceToSinkFlow")
                    connection.prepareStatement(DatabaseHelper.getMergeInto("vm_instance", "instanceId", "vmId", "hostNameId", "port")).use { statement ->
                        statement.setLong(1, instanceId)
                        statement.setLong(2, id)
                        statement.setInt(3, hostNameId)
                        statement.setInt(4, port)
                        statement.execute()
                    }
                }

                return if (id == 0L) null else VM(vmType, id, instanceId, name, groupName, hostName, port)
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error storing vm", e)
            return null
        }
    }

    private fun createVmSelectStatement(connection: Connection, vmType: VmType, instanceId: Long, nameId: Int, groupNameId: Int): PreparedStatement {
        return if (vmType == VmType.POOLED) {
            connection.prepareStatement("select vmId as id from vm_instance where instanceId=?").apply {
                setLong(1, instanceId)
            }
        } else {
            connection.prepareStatement("select id from vm where nameId=? and groupNameId=? and vmType=?").apply {
                setInt(1, nameId)
                setInt(2, groupNameId)
                setInt(3, vmType.databaseId)
            }
        }
    }

    fun delete(vms: List<VM>) {
        try {
            dataSource.connection.use { connection ->
                connection.prepareStatement("delete from vm where id=?").use { statement ->
                    for (vm in vms) {
                        statement.setLong(1, vm.id)
                        statement.execute()
                    }
                }
            }
        } catch (e: Exception) {
            VmManagerImpl.SERVER_LOGGER.error("error deleting vms", e)
        }
    }

    fun getVmById(id: Long): VM? {
        if (id == 0L) {
            return null
        }

        var result = synchronized(vmIdCache) { vmIdCache.get(id) }
        if (result == null) {
            try {
                dataSource.connection.use { connection ->
                    nameManager.getQueryNameValueStatement(connection).use { queryNameValueStatement ->
                        connection.prepareStatement("select * from vm_instance where vmId=?").use { hostNameStatement ->
                            connection.prepareStatement("select * from vm where id=?").use { statement ->
                                statement.setLong(1, id)
                                statement.executeQuery().use { resultSet ->
                                    if (resultSet.next()) {
                                        result = addHostName(getVm(resultSet, queryNameValueStatement), hostNameStatement, queryNameValueStatement)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                VmManagerImpl.SERVER_LOGGER.error("error reading vm by id", e)
            }
            result?.let {
                synchronized(vmIdCache) { vmIdCache.put(id, it) }
            }
        }
        return result
    }
}
