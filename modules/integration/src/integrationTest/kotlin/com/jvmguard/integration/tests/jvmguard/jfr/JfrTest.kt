package com.jvmguard.integration.tests.jvmguard.jfr

import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.Controller
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.data.config.triggers.TimeUnit
import com.jvmguard.data.config.triggers.actions.JfrConfigMode
import com.jvmguard.data.config.triggers.actions.JfrDefaultProfile
import com.jvmguard.data.config.triggers.actions.RecordJfrAction
import com.jvmguard.data.file.SnapshotFileType

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

