package dev.jvmguard.integration.tests.jvmguard.jfr

import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.data.config.triggers.TimeUnit
import dev.jvmguard.data.config.triggers.actions.JfrConfigMode
import dev.jvmguard.data.config.triggers.actions.JfrDefaultProfile
import dev.jvmguard.data.config.triggers.actions.RecordJfrAction
import dev.jvmguard.data.file.SnapshotFileType

class JfrTest : JvmGuardTest() {

    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = VM_COUNT
    override fun isWaitForListener(runNo: Int, vmNo: Int, libraryNo: Int) = false
    override fun isFailOnLog(vmConfig: VMConfig, runNo: Int, vmNo: Int, libraryNo: Int) = vmConfig.isAtLeastJava(11)

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vms = waitForConnections(serverConnection)

        try {
            println("recording JFR")
            vms.forEach {
                serverConnection.recordJfr(it, getRecordJfrAction(10))
            }
            Thread.sleep(20 * 1000)
            if (isJfrSupported()) {
                assertEqual(serverConnection.getSnapshotFiles(SnapshotFileType.JFR, null).size, VM_COUNT)
            } else {
                assertEqual(serverConnection.getSnapshotFiles(SnapshotFileType.JFR, null).size, 0)
            }
        } finally {
            controller.finished()
        }
    }

    private fun isJfrSupported() = currentVmConfig.isAtLeastJava(11)

    fun getRecordJfrAction(seconds: Int) = RecordJfrAction().apply {
        time = seconds
        timeUnit = TimeUnit.SECONDS
        configMode = JfrConfigMode.PREDEFINED
        profileName = JfrDefaultProfile.PROFILE.toString()
    }

    companion object {
        const val VM_COUNT = 3
    }

}

