package dev.jvmguard.integration.tests.jvmguard.perf

import dev.jvmguard.agent.AgentConstants
import dev.jvmguard.agent.config.base.LogCategory
import dev.jvmguard.agent.config.transactions.DeclaredTransactionDef
import dev.jvmguard.agent.config.transactions.MatchedTransactionDef
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.config.triggers.PolicyTrigger
import dev.jvmguard.data.config.triggers.Trigger
import dev.jvmguard.data.config.triggers.actions.LogAction
import dev.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager

/**
 * Standalone throughput benchmark
 */
class BasePerfBenchmark : JvmGuardTest() {

    override fun getRunClassName(): String = BasePerfWorkload::class.java.name

    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) =
        super.getJvmGuardOptions(runNo, vmNo, libraryNo) + " -Xmx512m -XX:+HeapDumpOnOutOfMemoryError" +
            // Forward the iteration-count override to the workload child JVM (which reads it via Long.getLong).
            (System.getProperty("jvmguard.benchmark.repetition")?.let { " -Djvmguard.benchmark.repetition=$it" } ?: "")

    override fun getVmName(vmNo: Int) = "test_vm"
    override fun getGroupName(vmNo: Int) = "group"
    override fun isWaitForListener(runNo: Int, vmNo: Int, libraryNo: Int) = false

    // The default 50M-iteration run takes minutes; keep the watchdog generous.
    override fun getRunTimeoutSeconds() = 60 * 60

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.transactionSettings.transactionDefs.clear()
        rootConfig.triggerSettings.triggers.clear()

        val matchedDef = MatchedTransactionDef()
        matchedDef.initDefault()
        matchedDef.interceptionTarget = MatchedTransactionDef.InterceptionTarget.METHOD
        matchedDef.declaringClassName = BasePerfWorkload::class.java.name
        matchedDef.methodName = "pojoExecute"
        matchedDef.methodSignature = "()V"
        rootConfig.transactionSettings.transactionDefs.add(matchedDef)

        val declaredDef = DeclaredTransactionDef()
        declaredDef.initDefault()
        rootConfig.transactionSettings.transactionDefs.add(declaredDef)

        val threshold = Threshold()
        threshold.telemetryIdentifier = PersistentTelemetryIdentifier("cu", "", AgentConstants.TELEMETRY_TYPE_DECLARED, "test1")
        threshold.lowerBound = 10
        rootConfig.thresholdSettings.thresholds.add(threshold)

        val trigger = PolicyTrigger()
        trigger.count = 1
        trigger.isSlow = true
        trigger.interval = Trigger.Interval.MINUTE
        trigger.triggerActions.add(LogAction(LogCategory.WARNING, "Test warning"))
        rootConfig.triggerSettings.triggers.add(trigger)
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        waitForConnection(serverConnection, listOf(getVmName(1)))
        println("BENCHMARK: workload connected; running timed loop (this can take minutes)...")
        // The workload calls System.exit(0) when done, which disconnects it from the server.
        while (serverConnection.connectedVms.isNotEmpty()) {
            sleep(1000)
        }
        println("BENCHMARK: workload finished. The timing (a 'BENCHMARK RESULT:' line) is in the workload " +
            "console log under ${controller.workingDir.parentFile}; per-run timings are in the *.perf file there.")
    }
}
