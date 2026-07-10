package com.jvmguard.integration.tests.jvmguard.jprofiler

import com.jvmguard.data.config.triggers.TimeUnit
import com.jvmguard.data.config.triggers.actions.RecordJpsAction
import com.jvmguard.collector.jprofiler.JProfilerPackageRepository
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.integration.Controller
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import com.jvmguard.integration.config.VMConfig

/**
 * Exercises the "record JProfiler snapshot" action end to end: the server resolves and caches the
 * JProfiler agent package, transfers it to the monitored JVM, loads it via jpenable, records CPU and
 * saves a `.jps` snapshot which is delivered to the inbox.
 */
class JProfilerTest : JvmGuardTest() {

    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = VM_COUNT
    override fun isWaitForListener(runNo: Int, vmNo: Int, libraryNo: Int) = false
    override fun isFailOnLog(vmConfig: VMConfig, runNo: Int, vmNo: Int, libraryNo: Int) = false

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        // TODO remove when 16.1.2 is released
        System.setProperty(JProfilerPackageRepository.VERSION_OVERRIDE_PROPERTY, "16.1.2")
        val vms = waitForConnections(serverConnection)
        try {
            println("recording JProfiler snapshot")
            vms.forEach { serverConnection.recordJps(it, recordJpsAction(RECORDING_SECONDS)) }

            // Allow time for: package download (~9 MB) + transfer + jpenable attach + recording + transfer back.
            Thread.sleep(120 * 1000)

            val snapshots = serverConnection.getSnapshotFiles(SnapshotFileType.JPS, null)
            assertEqual(snapshots.size, VM_COUNT)
            // A real recorded snapshot is well over 1 KB; guard against an empty/placeholder file.
            snapshots.forEach {
                if (serverConnection.getSnapshotFileSize(it) < 1000L) {
                    throw AssertionError("JProfiler snapshot is too small: ${serverConnection.getSnapshotFileSize(it)} bytes")
                }
            }
        } finally {
            controller.finished()
        }
    }

    private fun recordJpsAction(seconds: Int) = RecordJpsAction().apply {
        time = seconds
        timeUnit = TimeUnit.SECONDS
        isCreateInboxItem = true
    }

    companion object {
        const val VM_COUNT = 1
        const val RECORDING_SECONDS = 5
    }
}
