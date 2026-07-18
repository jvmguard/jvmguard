package dev.jvmguard.integration.tests.jvmguard.previous

import dev.jvmguard.agent.JvmGuardAgent
import dev.jvmguard.agent.config.VmType
import dev.jvmguard.agent.config.telemetry.MBeanLineConfig
import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.integration.JvmGuardTest
import dev.jvmguard.integration.Controller
import dev.jvmguard.integration.TestServerConnection
import dev.jvmguard.integration.TestVmManager
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.integration.tests.jvmguard.dashboard.toBigDecimal
import dev.jvmguard.integration.util.TimeComparator
import dev.jvmguard.integration.util.TransactionTreeComparator
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.file.SnapshotFileType
import dev.jvmguard.data.transactions.TransactionDataType
import dev.jvmguard.data.transactions.TransactionTreeInterval
import dev.jvmguard.data.vmdata.*
import java.io.File

/**
 *  To re-enable backward-compat testing see modules/integration/previous/README.md
 */
class PreviousAgentTest : JvmGuardTest() {

    // previous agent versions, each must exist under modules/integration/previous/<version>/.
    private val previousVersions = emptyList<String>()

    override fun isRunOnVM(vmConfig: VMConfig) = vmConfig.isJava(8) && previousVersions.isNotEmpty()
    override fun isCleanUserDir(libraryNo: Int) = libraryNo < 3
    override fun getJvmGuardOptions(runNo: Int, vmNo: Int, libraryNo: Int) =
        super.getJvmGuardOptions(runNo, vmNo, libraryNo) + (if (libraryNo < 3 && (vmNo == 2 || vmNo == 3)) " -Djvmguard.ignoreConfig=true" else "")

    override fun getServerOptions(runNo: Int, libraryNo: Int): Map<Any, Any> =
        super.getServerOptions(runNo, libraryNo).let {
            if (libraryNo == 2) {
                it + mapOf("jvmguard.int.noTempFileTest" to true.toString())
            } else {
                it
            }
        }

    // VM 1 runs the current agent; the rest run the pinned previous versions.
    override fun getVmCount(vmConfig: VMConfig, runNo: Int) = 1 + previousVersions.size
    override fun isInitListener(runNo: Int, vmNo: Int, libraryNo: Int) = false

    override fun modifyInitialRootConfig(rootConfig: GroupConfig) {
        rootConfig.telemetrySettings.mbeanTelemetries.add(MBeanTelemetryConfig("mbean tel1", TelemetryUnit.PLAIN, 0, true, false).apply {
            lines.add(MBeanLineConfig("java.lang:type=ClassLoading", "TotalLoadedClassCount", "class"))
        })
    }

    override fun replaceAgent(vmNo: Int, libraryNo: Int, defaultAgent: File) = if (vmNo == 1) {
        defaultAgent
    } else {
        val previousAgentsDir = File(defaultAgent.parentFile.parentFile.parentFile, "modules/integration/previous")
        File(previousAgentsDir, "${previousVersions[vmNo - 2]}/jvmguard.jar")
    }

    override fun connect(vmManager: TestVmManager, serverConnection: TestServerConnection, controller: Controller) {
        val vms = waitForConnections(serverConnection)

        sleep(70 * 1000)
        val heapType =
            serverConnection.idToTelemetryType.values.find { it.telemetryIdentifier == PersistentTelemetryIdentifier("hp", TelemetryType.SUB_ID_USED_HEAP) }!!
        val declaredType = serverConnection.idToTelemetryType.values.find { it.name == "test1" }!!
        val mbeanType = serverConnection.idToTelemetryType.values.find { it.name == "mbean tel1 (class)" }!!

        val vmDataHolderGroup = serverConnection.getVmDataHolders(VmFilter.CONNECTED, SparkLineRange.LAST_HOUR, listOf(heapType, declaredType, mbeanType))
        val dataMap = vmDataHolderGroup.groupChildren[VmIdentifier(getGroupName(1), VmType.GROUP)]!!.vmDataMap

        vms.forEach { vm ->
            checkTree(
                serverConnection,
                TransactionTreeInterval.HOUR,
                TransactionDataType.TRANSACTION,
                1,
                TransactionTreeComparator(TimeComparator.FIFTY_PERCENT),
                vm
            )

            val vmDataHolder = dataMap[vm]!!

            assertTrue(vmDataHolder.getSparkLineData(heapType).scaledCurrent.toBigDecimal() > 0.toBigDecimal())
            assertEqual(vmDataHolder.getSparkLineData(declaredType).scaledCurrent, 10.toBigDecimal())

            if (libraryNo == 3 || vm.name == getVmName(1)) {
                println("CHECKING PATH1")
                assertFalse(vmDataHolder.isOutdatedAgent)
                assertTrue(vmDataHolder.getSparkLineData(mbeanType).scaledCurrent.toBigDecimal() > 0.toBigDecimal())
                assertTrue(serverConnection.getMBeanNames(vm, false).isNotEmpty())
            } else {
                println("CHECKING PATH2 ${vm.name} ${getVmName(1)}")
                assertTrue(vmDataHolder.isOutdatedAgent)
                assertTrue(vmDataHolder.getSparkLineData(mbeanType).scaledCurrent.toBigDecimal() > 0.toBigDecimal())
                assertTrue(serverConnection.getMBeanNames(vm, false).isNotEmpty())
            }
            serverConnection.heapDump(vm)
        }

        sleep(30 * 1000)
        assertEqual(serverConnection.getSnapshotFiles(SnapshotFileType.HPZ, null).size, getVmCount(connectVmConfig, 1))

        val jvmguardUserDir = File(System.getProperty("user.home"), ".jvmguard")
        assertTrue(jvmguardUserDir.isDirectory)
        assertTrue(
            File(
                jvmguardUserDir,
                "agent/" + BootstrapFileUtil.getHashedPath(File(System.getProperty("distDir") + "/agent").canonicalPath) + "/" + JvmGuardAgent.getBuildVersion() + "/agent.jar"
            ).isFile
        )
        // Verify each previous-agent VM downloaded and cached its agent
        (2..getVmCount(connectVmConfig, 1)).forEach { checkDownloadedAgent(jvmguardUserDir, it) }
    }

    private fun checkDownloadedAgent(jvmguardUserDir: File, vmNo: Int) {
        assertTrue(
            File(
                jvmguardUserDir,
                "agent/" + BootstrapFileUtil.getHashedPath(
                    replaceAgent(
                        vmNo,
                        1,
                        File(System.getProperty("distDir") + "/agent/jvmguard.jar")
                    ).parentFile.canonicalPath
                ) + "/" + JvmGuardAgent.getBuildVersion() + "/agent.jar"
            ).isFile
        )
    }

}
