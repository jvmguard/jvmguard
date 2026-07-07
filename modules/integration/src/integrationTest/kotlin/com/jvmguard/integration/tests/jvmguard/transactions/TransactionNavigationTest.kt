package com.jvmguard.integration.tests.jvmguard.transactions

import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.config.transactions.DeclaredTransactionDef
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.integration.util.TimeComparator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.common.helper.Direction
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier

class TransactionNavigationTest : JvmGuardTest() {
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 2

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.add(DeclaredTransactionDef().apply {
            initDefault()
            group.usedValue = "sample"
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vm = waitForConnections(serverConnection).first()

        waitForNextConfigRequest(serverConnection)
        checkTree(
            serverConnection,
            TransactionTreeInterval.HOUR,
            TransactionDataType.TRANSACTION,
            1,
            true,
            TransactionTreeComparator(TimeComparator.THIRTY_PERCENT)
        )

        val time = serverConnection.currentTime
        checkMove(serverConnection, time, null)
        checkMove(serverConnection, time, vm)
        var cursor = serverConnection.getCurrentTransactionTreeCursor(null, TransactionTreeInterval.MINUTE, TransactionDataType.TRANSACTION)
        cursor = serverConnection.changeTransactionCursor(cursor, cursor.vm, TransactionTreeInterval.TEN_MINUTE)
        cursor = serverConnection.changeTransactionCursor(cursor, vm, TransactionTreeInterval.TEN_MINUTE)
        cursor = serverConnection.changeTransactionCursor(cursor, vm, TransactionTreeInterval.HOUR)
        assertTrue(cursor.interval == TransactionTreeInterval.HOUR) {
            println(cursor)
        }

        assertEqual(serverConnection.namedVms.size, 4)
        assertFalse(serverConnection.deleteVM(vm))
        terminate(vmManager, vm)
        sleep(5000)
        assertTrue(serverConnection.deleteVM(vm))
        sleep(1000)
        assertEqual(serverConnection.namedVms.size, 3)
        assertFalse(serverConnection.deleteVM(vmManager.getGroupVM(VmIdentifier(getGroupName(1), VmType.GROUP))))
        terminate(vmManager, serverConnection.connectedVms.first())
        sleep(5000)
        assertTrue(serverConnection.deleteVM(vmManager.getGroupVM(VmIdentifier(getGroupName(1), VmType.GROUP))))
        sleep(1000)
        assertEqual(serverConnection.namedVms.size, 1)
        println("finished")
    }

    private fun checkMove(serverConnection: TestServerConnection, time: Long, vm: VM?) {
        var cursor = serverConnection.getTransactionTreeCursor(vm, TransactionTreeInterval.MINUTE, TransactionDataType.TRANSACTION, time - 1000 * 60 * 1000)
        assertTrue(cursor.availability.isAvailable)
        assertFalse(cursor.isLatest)
        assertTrue(cursor.startTime > time - 1000 * 60 * 10) {
            println("${cursor.startTime} ${time - 1000 * 60 * 10}")
        }

        val previousCursor = serverConnection.moveTransactionTreeCursor(cursor, Direction.PREVIOUS)
        assertFalse(previousCursor.availability.isAvailable)
        assertFalse(previousCursor.isLatest)
        val nextCursor = serverConnection.moveTransactionTreeCursor(previousCursor, Direction.NEXT)
        assertTrue(nextCursor == cursor) {
            println(nextCursor)
            println(cursor)
        }
        assertTrue(nextCursor == serverConnection.moveTransactionTreeCursor(nextCursor, Direction.CURRENT)) {
            println(nextCursor)
            println(cursor)
        }
        cursor = serverConnection.moveTransactionTreeCursor(cursor, Direction.NEXT)

        var count = 2
        while (!cursor.isLatest) {
            cursor = serverConnection.moveTransactionTreeCursor(cursor, Direction.NEXT)
            count++
        }
        assertBetween(count, 3, 9)
    }
}
