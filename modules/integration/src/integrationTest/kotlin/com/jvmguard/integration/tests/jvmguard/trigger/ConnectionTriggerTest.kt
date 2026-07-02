package com.jvmguard.integration.tests.jvmguard.trigger

import com.jvmguard.agent.config.VmType
import com.jvmguard.agent.config.base.LogCategory
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.triggers.ConnectionTrigger
import com.jvmguard.data.config.triggers.Trigger
import com.jvmguard.data.config.triggers.actions.InboxAction
import com.jvmguard.data.config.triggers.actions.LogAction
import com.jvmguard.data.config.triggers.actions.TextAction
import com.jvmguard.data.vmdata.VM
import com.jvmguard.data.vmdata.VmIdentifier

class ConnectionTriggerTest : JvmGuardTest() {

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx64m"
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 10

    override fun getInitialGroupConfigs(): List<GroupConfig> {
        val subConfig = GroupConfig.createDefault(VmIdentifier(getGroupName(1), VmType.GROUP))

        subConfig.triggerSettings.triggers.add(ConnectionTrigger().apply {
            count = 5
            startMode = ConnectionTrigger.StartMode.IMMEDIATELY
            triggerActions.add(InboxAction("sub period 5"))
            triggerActions.add(LogAction(LogCategory.INFO, triggerActions.filterIsInstance<TextAction>().last().text))
        })

        subConfig.triggerSettings.triggers.add(ConnectionTrigger().apply {
            count = 5
            startMode = ConnectionTrigger.StartMode.REACHED_ONLY
            minimumTime = 0
            triggerActions.add(InboxAction("sub reached 5"))
            triggerActions.add(LogAction(LogCategory.INFO, triggerActions.filterIsInstance<TextAction>().last().text))
        })

        subConfig.triggerSettings.triggers.add(ConnectionTrigger().apply {
            count = 1
            startMode = ConnectionTrigger.StartMode.REACHED_ONLY
            minimumTime = 0
            triggerActions.add(InboxAction("sub reached 1"))
            triggerActions.add(LogAction(LogCategory.INFO, triggerActions.filterIsInstance<TextAction>().last().text))
        })

        subConfig.triggerSettings.triggers.add(ConnectionTrigger().apply {
            count = 25
            startMode = ConnectionTrigger.StartMode.IMMEDIATELY
            minimumTime = 0
            inhibitionInterval = Trigger.Interval.MINUTE
            inhibitionTime = 1
            triggerActions.add(InboxAction("sub period 25"))
            triggerActions.add(LogAction(LogCategory.INFO, triggerActions.filterIsInstance<TextAction>().last().text))
        })

        return listOf(GroupConfig.createDefault(), subConfig)
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vms = ArrayList<VM>(waitForConnections(serverConnection))

        sleep(1000 * 10)

        modifyCurrentRootConfig(serverConnection) { rootConfig ->
            rootConfig.triggerSettings.triggers.add(ConnectionTrigger().apply {
                count = 5
                startMode = ConnectionTrigger.StartMode.IMMEDIATELY
                triggerActions.add(InboxAction("period 5"))
                triggerActions.add(LogAction(LogCategory.INFO, triggerActions.filterIsInstance<TextAction>().last().text))
            })

            rootConfig.triggerSettings.triggers.add(ConnectionTrigger().apply {
                count = 5
                startMode = ConnectionTrigger.StartMode.REACHED_ONLY
                minimumTime = 0
                triggerActions.add(InboxAction("reached 5"))
                triggerActions.add(LogAction(LogCategory.INFO, triggerActions.filterIsInstance<TextAction>().last().text))
            })

            rootConfig.triggerSettings.triggers.add(ConnectionTrigger().apply {
                count = 1
                startMode = ConnectionTrigger.StartMode.REACHED_ONLY
                triggerActions.add(InboxAction("reached 1"))
                minimumTime = 0
                triggerActions.add(LogAction(LogCategory.INFO, triggerActions.filterIsInstance<TextAction>().last().text))
            })

            rootConfig.triggerSettings.triggers.add(ConnectionTrigger().apply {
                count = 25
                startMode = ConnectionTrigger.StartMode.REACHED_ONLY
                minimumTime = 0
                inhibitionInterval = Trigger.Interval.MINUTE
                inhibitionTime = 1
                triggerActions.add(InboxAction("reached 25"))
                triggerActions.add(LogAction(LogCategory.INFO, triggerActions.filterIsInstance<TextAction>().last().text))
            })
        }
        sleep(1000 * 90)

        println("check1")
        var inboxItems = serverConnection.inboxItems
        assertBetween(inboxItems.size, 1, 4)
        assertEqual(
            inboxItems.filter { it.snapshotFileType == null && it.name == "Trigger on VM group default" && it.message == "sub period 25" }.size,
            inboxItems.size
        )

        println("disconnect")
        for (i in 0 until 6) {
            terminate(vmManager, vms[i], false)
        }

        sleep(1000 * 60 * 2)

        println("check2")
        inboxItems = serverConnection.inboxItems
        var repeatedCount = inboxItems.filter { it.snapshotFileType == null && it.name == "Trigger on VM group default" && it.message == "sub period 25" }.size

        assertBetween(repeatedCount, 2, 5)
        assertEqual(inboxItems.size, repeatedCount + 4)
        assertTrue(inboxItems.find { it.snapshotFileType == null && it.name == "Trigger on VM group default" && it.message == "sub period 5" } != null)
        assertTrue(inboxItems.find { it.snapshotFileType == null && it.name == "Trigger on VM group default" && it.message == "sub reached 5" } != null)
        assertTrue(inboxItems.find { it.snapshotFileType == null && it.name == "Trigger on root group" && it.message == "period 5" } != null)
        assertTrue(inboxItems.find { it.snapshotFileType == null && it.name == "Trigger on root group" && it.message == "reached 5" } != null)

        println("disconnect")
        for (i in 6 until 10) {
            terminate(vmManager, vms[i], false)
        }
        sleep(1000 * 30)

        println("check3")
        inboxItems = serverConnection.inboxItems
        repeatedCount = inboxItems.filter { it.snapshotFileType == null && it.name == "Trigger on VM group default" && it.message == "sub period 25" }.size
        assertBetween(repeatedCount, 2, 6)
        assertEqual(inboxItems.size, repeatedCount + 6)
        assertTrue(inboxItems.find { it.snapshotFileType == null && it.name == "Trigger on VM group default" && it.message == "sub reached 1" } != null)
        assertTrue(inboxItems.find { it.snapshotFileType == null && it.name == "Trigger on root group" && it.message == "reached 1" } != null)
        println("ok")
    }
}
