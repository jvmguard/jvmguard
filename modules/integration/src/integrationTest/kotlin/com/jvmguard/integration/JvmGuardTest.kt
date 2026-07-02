package com.jvmguard.integration

import com.jvmguard.common.helper.ListModification
import com.jvmguard.common.util.StringUtil
import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.GroupConfig
import com.jvmguard.data.config.SmtpConfig
import com.jvmguard.data.transactions.TransactionDataType
import com.jvmguard.data.transactions.TransactionTree
import com.jvmguard.data.transactions.TransactionTreeInterval
import com.jvmguard.data.vmdata.TelemetryInterval
import com.jvmguard.data.vmdata.VM
import com.jvmguard.integration.config.VMConfig
import com.jvmguard.integration.util.ExportHelper
import com.jvmguard.integration.util.TransactionTreeCalculator
import com.jvmguard.integration.util.TransactionTreeComparator
import com.jvmguard.integration.util.TransactionTreeReader
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.util.EnumSet

/**
 * Base class for agent integration tests. A subclass overrides [modifyInitialRootConfig] to set up the
 * server config and [connect] to perform assertions against the backend while an agent-instrumented
 * workload runs in a child JVM.
 */
@ExtendWith(AgentIntegrationExtension::class)
abstract class JvmGuardTest {

    private var nextConfigRequest = FIRST_CONFIG_REQUEST
    private var lastRecordedIndex = 0

    internal lateinit var controller: Controller
    @Volatile internal var abort = false
    internal lateinit var vmConfig: VMConfig
    internal lateinit var connectVmConfig: VMConfig
    internal var runNo = 1
    internal var libraryNo = 1

    /** Injected by [AgentIntegrationExtension] before the test is executed. */
    internal lateinit var fixture: AgentFixture

    fun setCurrentRun(vmConfig: VMConfig, runNo: Int, libraryNo: Int) {
        this.vmConfig = vmConfig
        this.connectVmConfig = vmConfig
        this.runNo = runNo
        this.libraryNo = libraryNo
        this.nextConfigRequest = FIRST_CONFIG_REQUEST
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @JdkSource
    fun run(jdk: JdkUnderTest) {
        val candidate = VMConfig(jdk.majorVersion)
        // A test may opt out of a given JDK at runtime (beyond the @MinJdk floor enforced by @JdkSource),
        // in which case it should be reported as skipped.
        Assumptions.assumeTrue(isRunOnVM(candidate)) { "${javaClass.simpleName} does not run on ${candidate.name}" }
        fixture.execute(this, jdk)
    }

    // --- configuration hooks (overridden by the tests) ------------------------------------------

    open fun getInitialGroupConfigs(): List<GroupConfig> {
        val groupConfig = GroupConfig.createDefault()
        modifyInitialRootConfig(groupConfig)
        return listOf(groupConfig)
    }

    open fun modifyInitialRootConfig(rootConfig: GroupConfig) {}

    open fun modifyInitialGlobalConfig(globalConfig: GlobalConfig) {}

    /** Extra JVM options for the workload child JVM. */
    open fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) = ""

    open fun getVmCount(vmConfig: VMConfig, runNo: Int) = 1

    open fun getRunCount(vmConfig: VMConfig) = 1

    open fun isRunOnVM(vmConfig: VMConfig) = true

    open fun modifyEncryption(smtpConfig: SmtpConfig) {}

    open fun getVmName(vmNo: Int) = "JVM" + (if (vmNo == 1) "" else vmNo)

    open fun isPool(vmNo: Int) = false

    open fun getGroupName(vmNo: Int) = "default"

    open val httpPort get() = 8010

    /** Fully-qualified name of the workload class. */
    open fun getRunClassName(): String = javaClass.name.replace(Regex("Test$"), "Workload")

    open fun isCleanUserDir(libraryNo: Int) = true

    open fun isWaitForListener(runNo: Int, vmNo: Int, libraryNo: Int) = true

    open fun isInitListener(runNo: Int, vmNo: Int, libraryNo: Int) = true

    open fun isFailOnLog(vmConfig: VMConfig, runNo: Int, vmNo: Int, libraryNo: Int) = true

    open val currentVmConfig get() = vmConfig

    /** Extra server-side system properties that are applied before `ServerMain` is executed. */
    open fun getServerOptions(runNo: Int, libraryNo: Int): Map<Any, Any> = emptyMap()

    /** Overall per-test timeout in seconds. */
    open fun getRunTimeoutSeconds() = 60 * 25

    open fun replaceAgent(vmNo: Int, libraryNo: Int, defaultAgent: File) = defaultAgent

    fun setController(controller: Controller) {
        this.controller = controller
    }

    open fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {}

    // --- helpers driven from connect() ----------------------------------------------------------

    fun modifyCurrentRootConfig(serverConnection: TestServerConnection, closure: (GroupConfig) -> Unit) {
        for (groupConfig in serverConnection.groupConfigs) {
            if (groupConfig.hierarchyPath.isEmpty()) {
                closure(groupConfig)
                serverConnection.modifyGroupConfigs(
                    ListModification(arrayListOf(groupConfig), emptyList(), emptyList(), GroupConfig::class.java))
                return
            }
        }
    }

    fun waitForConnections(serverConnection: TestServerConnection): Collection<VM> {
        println("WAITING FOR VMS")
        while (true) {
            if (serverConnection.connectedVms.size == getVmCount(vmConfig, runNo)) {
                println("VMS CONNECTED")
                return serverConnection.connectedVms
            }
            sleep(100)
        }
    }

    fun waitForConnection(serverConnection: TestServerConnection, vmNames: List<String>): Collection<VM> {
        println("WAITING FOR VMS $vmNames")
        while (true) {
            val connectedVms = serverConnection.connectedVms
            if (connectedVms.size >= vmNames.size) {
                val waitVms = mutableListOf<VM>()
                val allFound = vmNames.all { searchedName ->
                    val match = connectedVms.firstOrNull { it.name == searchedName }
                    match?.also { waitVms.add(it) } != null
                }
                if (allFound) {
                    println("VMS CONNECTED")
                    return waitVms
                }
            }
            sleep(100)
        }
    }

    fun waitForNextConfigRequest(serverConnection: TestServerConnection, vms: Collection<VM>? = null) {
        waitForConfigRequest(serverConnection, nextConfigRequest++, vms)
    }

    fun waitForConfigRequest(serverConnection: TestServerConnection, num: Int, vms: Collection<VM>?) {
        println("WAITING FOR CONFIG REQUEST $num")
        val targetVms = vms ?: serverConnection.connectedVms
        while (!checkTelemetryValue(serverConnection, num, targetVms)) {
            sleep(1000)
        }
        println("FOUND CONFIG REQUEST $num")
    }

    private fun checkTelemetryValue(serverConnection: TestServerConnection, num: Int, vms: Collection<VM>): Boolean =
        vms.all { checkTelemetryValue(serverConnection, it, "requestedConfigurationNumber", num) }

    fun checkTelemetryValue(serverConnection: TestServerConnection, vm: VM, name: String, value: Int): Boolean {
        for (identifier in serverConnection.customTelemetryNodes) {
            if (identifier.name == name) {
                val telemetryData = serverConnection.getCustomTelemetryData(vm, identifier, TelemetryInterval.TEN_MINUTES, System.currentTimeMillis())
                val data = requireNotNull(telemetryData.rootNode).calculateUnitScale(FrequencyUnit.PER_MINUTE).data
                if (data.isNotEmpty()) {
                    val values = data.first().unitScaledData
                    val lastValue = values?.lastOrNull()
                    if (lastValue != null && lastValue.toInt() == value) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun checkTree(serverConnection: TestServerConnection, transactionTreeInterval: TransactionTreeInterval, transactionDataType: TransactionDataType, index: Int, immediate: Boolean, treeComparator: TransactionTreeComparator, vm: VM? = null) {
        checkTree(serverConnection, transactionTreeInterval, EnumSet.of(transactionDataType), index, immediate, treeComparator, vm)
    }

    fun checkTree(serverConnection: TestServerConnection, transactionTreeInterval: TransactionTreeInterval, transactionDataTypes: EnumSet<TransactionDataType>, index: Int, immediate: Boolean, treeComparator: TransactionTreeComparator, vm: VM? = null) {
        if (isRecordMode) {
            println("RECORD MODE")
            if (lastRecordedIndex != index) {
                sleep(80 * 1000L)
            }
            lastRecordedIndex = index
            for (transactionDataType in transactionDataTypes) {
                exportSingleTree(serverConnection, transactionTreeInterval, transactionDataType, index, treeComparator, vm)
            }
        } else {
            if (!immediate) {
                sleep(80 * 1000L)
            }
            for (transactionDataType in transactionDataTypes) {
                checkSingleTree(index, immediate, serverConnection, transactionTreeInterval, transactionDataType, treeComparator, vm)
            }
        }
    }

    fun checkTree(transactionTree: TransactionTree, suffix: String, treeComparator: TransactionTreeComparator) {
        if (isRecordMode) {
            ExportHelper.exportTree(transactionTree, File(controller.workingDir, javaClass.simpleName + suffix).absolutePath, treeComparator.isIncludeTime, treeComparator.timeUnit)
        } else {
            println("comparing $suffix")
            val expectedTree = TransactionTreeReader.read(getXmlStream(suffix))
            if (!treeComparator.isEqual(expectedTree, transactionTree)) {
                println(transactionTree)
                throw AssertionError()
            }
        }
    }

    val isRecordMode get() = "yes" == System.getenv("JVMGUARD_RECORD") || java.lang.Boolean.getBoolean("jvmguard.record")

    fun recordFile(fileName: String, content: String): Boolean {
        if (!isRecordMode) {
            return false
        }
        File(controller.workingDir, fileName).writeText(content)
        return true
    }

    protected fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (_: InterruptedException) {
            Thread.interrupted()
            if (abort) {
                throw RuntimeException("aborted")
            }
        }
        if (abort) {
            throw RuntimeException("aborted")
        }
    }

    private fun exportSingleTree(serverConnection: TestServerConnection, transactionTreeInterval: TransactionTreeInterval, transactionDataType: TransactionDataType, index: Int, treeComparator: TransactionTreeComparator, vm: VM?) {
        val transactionCursor = serverConnection.getCurrentTransactionTreeCursor(vm, transactionTreeInterval, transactionDataType)
        val transactionTree = serverConnection.getCallTree(transactionCursor, false).transactionTree

        val namePrefix = javaClass.simpleName + getSuffix(transactionDataType, vm)
        if (treeComparator.isDifferential) {
            subtractTree(transactionTree, index) { number ->
                TransactionTreeReader.read(File(controller.workingDir, "$namePrefix$number.xml").absolutePath)
            }
        }
        ExportHelper.exportTree(transactionTree, File(controller.workingDir, namePrefix + index).absolutePath, treeComparator.isIncludeTime, treeComparator.timeUnit)
    }

    private fun checkSingleTree(index: Int, immediate: Boolean, serverConnection: TestServerConnection, transactionTreeInterval: TransactionTreeInterval, transactionDataType: TransactionDataType, treeComparator: TransactionTreeComparator, vm: VM?) {
        val expectedTree = TransactionTreeReader.read(getXmlStream(getSuffix(transactionDataType, vm) + index))
        println("EXPECTED TREE $expectedTree")

        repeat(if (immediate) 24 else 1) {
            val transactionCursor = serverConnection.getCurrentTransactionTreeCursor(vm, transactionTreeInterval, transactionDataType)
            val transactionTree = serverConnection.getCallTree(transactionCursor, false).transactionTree
            if (treeComparator.isDifferential) {
                subtractTree(transactionTree, index) { number ->
                    TransactionTreeReader.read(getXmlStream(getSuffix(transactionDataType, vm) + number))
                }
            }
            println(transactionTree)
            if (treeComparator.isEqual(expectedTree, transactionTree)) {
                return
            }
            sleep(5000)
        }
        throw AssertionError()
    }

    protected fun getSuffix(transactionDataType: TransactionDataType, vm: VM?): String {
        var suffix = if (transactionDataType == TransactionDataType.TRANSACTION) {
            ""
        } else {
            requireNotNull(StringUtil.capitalizeFirstLetter(transactionDataType.name.lowercase()))
        }
        if (vm != null) {
            suffix += "_${vm.name}_"
        }
        return suffix
    }

    fun getXmlStream(suffix: String): InputStream = getXmlStream(suffix, javaClass)

    fun getXmlStream(suffix: String, baseClass: Class<*>): InputStream =
        getStream(baseClass.simpleName + suffix + ".xml", baseClass)

    fun getStream(fileName: String): InputStream = getStream(fileName, javaClass)

    fun getStream(fileName: String, baseClass: Class<*>): InputStream {
        println("checking data/$fileName")
        return requireNotNull(baseClass.getResourceAsStream("data/$fileName"))
    }

    protected fun terminate(vmManager: TestVmManager, vm: VM, closeConnection: Boolean = true) {
        try {
            vmManager.terminate(vm, closeConnection)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    // --- assertion helpers (inherited and called unqualified by the tests) ----------------------

    fun assertEqual(n1: Any?, n2: Any?, errorOutput: (() -> Unit)? = null) {
        var v1 = n1
        var v2 = n2
        if (v1 is Number && v2 is Number) {
            if (v1 is Long || v1 is Int) {
                v1 = v1.toLong()
                v2 = v2.toLong()
            } else {
                v1 = v1.toDouble()
                v2 = v2.toDouble()
            }
        }
        val equal: Boolean
        var v1String = v1.toString()
        var v2String = v2.toString()
        @Suppress("IntroduceWhenSubject")
        when {
            v1 is Array<*> && v2 is Array<*> -> {
                equal = v1.contentDeepEquals(v2)
                v1String = v1.contentDeepToString()
                v2String = v2.contentDeepToString()
            }
            v1 is IntArray && v2 is IntArray -> {
                equal = v1.contentEquals(v2)
                v1String = v1.contentToString()
                v2String = v2.contentToString()
            }
            v1 is ByteArray && v2 is ByteArray -> {
                equal = v1.contentEquals(v2)
                v1String = v1.contentToString()
                v2String = v2.contentToString()
            }
            else -> equal = v1 == v2
        }
        if (!equal) {
            errorOutput?.invoke()
            throw AssertionError("error: $v1String != $v2String")
        }
    }

    fun assertBetween(n1: Number, lower: Number, upper: Number, errorOutput: (() -> Unit)? = null) {
        val n1Decimal = BigDecimal(n1.toString())
        if (n1Decimal < BigDecimal(lower.toString())) {
            errorOutput?.invoke()
            throw AssertionError("error: $n1 < $lower")
        }
        if (n1Decimal > BigDecimal(upper.toString())) {
            errorOutput?.invoke()
            throw AssertionError("error: $n1 > $upper")
        }
    }

    fun assertSimilar(measured: Number, expected: Number, errorOutput: (() -> Unit)?, percentage: Double) {
        val max = 1 + percentage / 100
        val min = 1 - minOf(percentage, if (percentage < 300) 80.0 else 90.0) / 100
        val measuredDouble = measured.toDouble()
        val expectedDouble = expected.toDouble()
        if (measuredDouble * max < expectedDouble) {
            errorOutput?.invoke()
            throw AssertionError("$measured * $max < $expected")
        } else if (measuredDouble * min > expectedDouble) {
            errorOutput?.invoke()
            throw AssertionError("$measured * $min > $expected")
        }
    }

    fun assertSimilar(measured: Number, expected: Number, errorOutput: (() -> Unit)? = null) =
        assertSimilar(measured, expected, errorOutput, similarPercentage)

    fun assertTrue(condition: Boolean, errorOutput: (() -> Unit)? = null) {
        if (!condition) {
            errorOutput?.invoke()
            throw AssertionError()
        }
    }

    fun assertFalse(condition: Boolean, errorOutput: (() -> Unit)? = null) = assertTrue(!condition, errorOutput)

    companion object {
        const val DEFAULT_SIMILAR_PERCENTAGE = 30.0
        var similarPercentage = DEFAULT_SIMILAR_PERCENTAGE

        private const val FIRST_CONFIG_REQUEST = 2

        private fun subtractTree(baseTree: TransactionTree, index: Int, retrieval: (Int) -> TransactionTree) {
            if (index > 1) {
                TransactionTreeCalculator.subtract(baseTree, retrieval(index - 1))
                subtractTree(baseTree, index - 1, retrieval)
            }
        }
    }
}
