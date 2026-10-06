package dev.jvmguard.integration.tests.jvmguard.tracing

import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.agent.config.transactions.DurationType
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.triggers.PolicyTrigger
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.config.triggers.actions.ThreadDumpAction
import dev.jvmguard.data.file.SnapshotFileType
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import java.io.File

// same setup as OtelCaptureTest, but with the OTel capture event flag turned off
class OtelCaptureDisabledTest : JvmGuardTest() {

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.first { it is DeclaredTransactionDef }.policy.apply {
            slowDurationType = DurationType.MILLIS
            slowValue = 100
            verySlowDurationType = DurationType.MILLIS
            verySlowValue = 10000
            overdueValue = 60000
        }
        rootConfig.triggerSettings.emitOtelCaptureEvents = false

        rootConfig.triggerSettings.triggers.add(PolicyTrigger().apply {
            id = 1
            filter = "*slowOperation"
            isSlow = true
            isVerySlow = false
            isOverdue = false
            isError = false
            count = 2
            interval = Trigger.Interval.NONE
            triggerActions.add(ThreadDumpAction())
        })
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        waitForConnection(serverConnection, listOf("JVM"))

        // the thread dump snapshot is the positive signal that the trigger capture actually fired
        val deadline = System.currentTimeMillis() + 5 * 60 * 1000
        var captureFired = false
        while (!captureFired && System.currentTimeMillis() < deadline && !abort) {
            captureFired = serverConnection.getSnapshotFiles(null, null).any { it.type == SnapshotFileType.THREAD_DUMP }
            if (!captureFired) {
                sleep(2000)
            }
        }
        assertTrue(captureFired) { println("no thread dump snapshot file recorded") }

        val workDir = File(System.getProperty("jvmguard.integration.workDir"))
            .listFiles()!!
            .first { it.isDirectory && it.name.startsWith("${javaClass.simpleName}-") }
        val consoleLog = workDir.resolve("JVM-run1-console.log")
        assertFalse(consoleLog.isFile && consoleLog.readLines().any { it.startsWith("OTEL-CAPTURE-RECORD") }) {
            println("OTEL-CAPTURE-RECORD line present despite disabled flag in $consoleLog")
        }
    }
}
