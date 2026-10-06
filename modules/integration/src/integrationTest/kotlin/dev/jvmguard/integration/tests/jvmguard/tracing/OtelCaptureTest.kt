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

class OtelCaptureTest : JvmGuardTest() {

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.first { it is DeclaredTransactionDef }.policy.apply {
            slowDurationType = DurationType.MILLIS
            slowValue = 100
            verySlowDurationType = DurationType.MILLIS
            verySlowValue = 10000
            overdueValue = 60000
        }

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

        val workDir = File(System.getProperty("jvmguard.integration.workDir"))
            .listFiles()!!
            .first { it.isDirectory && it.name.startsWith("${javaClass.simpleName}-") }
        val consoleLog = workDir.resolve("JVM-run1-console.log")

        val deadline = System.currentTimeMillis() + 5 * 60 * 1000
        var record: String? = null
        while (record == null && System.currentTimeMillis() < deadline && !abort) {
            record = consoleLog.takeIf { it.isFile }
                ?.readLines()
                ?.firstOrNull { it.startsWith("OTEL-CAPTURE-RECORD") }
            if (record == null) {
                sleep(2000)
            }
        }

        assertTrue(record != null) { println("no OTEL-CAPTURE-RECORD line in $consoleLog") }
        println(record)
        assertTrue(record!!.contains("body=jvmguard capture: thread dump"))
        assertTrue(record.contains("severity=INFO"))
        assertTrue(record.contains("jvmguard.capture.origin=trigger"))
        assertTrue(record.contains("jvmguard.capture.type=thread_dump"))
        assertTrue(record.contains("jvmguard.policy.event=SLOW"))
        assertTrue(record.contains("jvmguard.transaction.name=") && record.contains("slowOperation"))
        assertTrue(record.contains("jvmguard.trigger=Policy trigger [*slowOperation"))
        assertTrue(record.contains("jvmguard.vm.name=JVM"))

        assertTrue(serverConnection.getSnapshotFiles(null, null).any { it.type == SnapshotFileType.THREAD_DUMP }) {
            println("no thread dump snapshot file recorded")
        }
    }
}
