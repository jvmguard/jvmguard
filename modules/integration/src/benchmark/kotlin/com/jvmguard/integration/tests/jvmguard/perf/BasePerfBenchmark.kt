package com.jvmguard.integration.tests.jvmguard.perf

import com.jvmguard.agent.AgentConstants
import com.jvmguard.agent.config.base.LogCategory
import com.jvmguard.agent.config.transactions.DeclaredTransactionDef
import com.jvmguard.agent.config.transactions.MatchedTransactionDef
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.thresholds.Threshold
import com.jvmguard.data.config.triggers.PolicyTrigger
import com.jvmguard.data.config.triggers.Trigger
import com.jvmguard.data.config.triggers.actions.LogAction
import com.jvmguard.data.vmdata.PersistentTelemetryIdentifier
import com.jvmguard.integration.Controller
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager

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
