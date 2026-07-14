package dev.jvmguard.integration

import dev.jvmguard.agent.tools.importer.Importer
import dev.jvmguard.annotation.ClassTransaction
import dev.jvmguard.annotation.MethodTransaction
import dev.jvmguard.annotation.NoTransaction
import dev.jvmguard.annotation.Telemetry
import dev.jvmguard.collector.api.VmManager
import dev.jvmguard.collector.main.VmManagerImpl
import dev.jvmguard.common.config.ImportManager
import dev.jvmguard.common.helper.PasswordHelper
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.config.external.ServerInitConfig
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.User
import dev.jvmguard.integration.config.VMConfig
import dev.jvmguard.server.ServerMain
import dev.jvmguard.connector.api.MockMode
import dev.jvmguard.connector.api.Server
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.io.File
import java.io.IOException

/** Boots [ServerMain] in-process and runs one or more agent-instrumented workload child JVMs against it. */
class AgentFixture {

    private lateinit var vmManager: TestVmManager
    private lateinit var serverConnection: TestServerConnection
    private val processes = mutableListOf<Process>()

    fun execute(test: JvmGuardTest, jdk: JdkUnderTest) {
        val workDir = File(systemDir("jvmguard.integration.workDir"), "${test.javaClass.simpleName}-$jdk")
        deleteRecursively(workDir)
        val dataDir = File(workDir, "data")
        val outputDir = File(workDir, "output")
        dataDir.mkdirs()
        outputDir.mkdirs()

        val vmConfig = VMConfig(jdk.majorVersion)
        test.setCurrentRun(vmConfig, 1, 1)
        test.setController(ControllerImpl(outputDir))

        bootServer(test, dataDir)
        seedAdminSecurityContext()

        val watchdog = startWatchdog(test)
        try {
            // Some tests run the workload more than once.
            val runCount = test.getRunCount(vmConfig)
            for (runNo in 1..runCount) {
                System.setProperty("singleRunNo", runNo.toString())
                test.setCurrentRun(vmConfig, runNo, 1)
                processes.clear()
                for (vmNo in 1..test.getVmCount(vmConfig, runNo)) {
                    processes.add(launchWorkload(test, jdk, workDir, vmNo, runNo))
                }
                try {
                    test.connect(vmManager, serverConnection, test.controller)
                } finally {
                    terminateConnections()
                    destroyProcesses()
                }
            }
        } finally {
            watchdog.interrupt()
            shutdownServer()
        }
    }

    private fun shutdownServer() {
        try {
            ServerMain.closeContext()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun startWatchdog(test: JvmGuardTest): Thread {
        val main = Thread.currentThread()
        val timeoutMillis = test.getRunTimeoutSeconds() * 1000L
        return Thread({
            try {
                Thread.sleep(timeoutMillis)
                System.err.println("INTEGRATION TEST TIMEOUT after ${test.getRunTimeoutSeconds()}s")
                test.abort = true
                destroyProcesses()
                main.interrupt()
            } catch (_: InterruptedException) {
            }
        }, "integration-watchdog").apply {
            isDaemon = true
            start()
        }
    }

    private fun bootServer(test: JvmGuardTest, dataDir: File) {
        val jvmguardUserDir = File(System.getProperty("user.home"), ".jvmguard")
        if (test.isCleanUserDir(test.libraryNo) && !deleteRecursively(jvmguardUserDir) && jvmguardUserDir.exists()) {
            throw IOException("Could not delete $jvmguardUserDir")
        }

        System.setProperty("jvmguard.gcDays", "0")
        System.setProperty("jvmguard.gcStartMinutes", "0")
        System.setProperty("jvmguard.gcTimeFrame", "1500")
        System.setProperty("jvmguard.gcWaitTime", "0")
        System.setProperty("jvmguard.smtpTrustAllHosts", "true")
        System.setProperty("jvmguard.testControlFilter", "true")
        System.setProperty("jvmguard.httpPort", test.httpPort.toString())
        System.setProperty("jvmguard.vmPort", TEST_VM_PORT)
        System.setProperty("jvmguard.dataDirectory", dataDir.absolutePath)
        // From the test classpath the server cannot resolve the installation-layout logback.xml.
        val logbackFile = File(System.getProperty("jvmguard.integration.logbackFile", ""))
        if (logbackFile.isFile) {
            System.setProperty("logging.config", logbackFile.absolutePath)
        }
        for ((key, value) in test.getServerOptions(1, 1)) {
            System.setProperty(key.toString(), value.toString())
        }

        val configFile = createInitialConfig(test, dataDir)
        Importer().importFile(configFile, jvmguardUserDir)

        ServerMain.main(emptyArray())
        vmManager = TestVmManager(ServerMain.getBean(VmManager::class.java) as VmManagerImpl)
        val server = ServerMain.getBean(Server::class.java)
        serverConnection = TestServerConnection(server.login(LOGIN, PASSWORD, "", MockMode.NONE))
    }

    private fun launchWorkload(test: JvmGuardTest, jdk: JdkUnderTest, workDir: File, vmNo: Int, runNo: Int): Process {
        val agentJar = test.replaceAgent(vmNo, 1, File(systemDir("jvmguard.integration.distDir"), "agent/jvmguard.jar").canonicalFile)

        val vmName = test.getVmName(vmNo)
        val agentOptions = buildString {
            append("=port=").append(TEST_VM_PORT).append(",")
            append(if (test.isPool(vmNo)) "pool=" else "name=").append(vmName)
            if (test.getGroupName(vmNo).isNotEmpty()) {
                append(",group=").append(test.getGroupName(vmNo))
            }
            append(",keyStore=")
        }

        val cmd = buildList {
            add(jdk.javaExecutable)
            addAll(test.getJvmGuardOptions(1, vmNo, 1).trim().split(Regex("\\s+")).filter { it.isNotEmpty() })
            add("-javaagent:" + agentJar.canonicalPath + agentOptions)
            add("-Djvmguard.int.failOnLog=" + test.isFailOnLog(test.vmConfig, 1, vmNo, 1))
            add("-Djvmguard.saveInstrumented=true")
            add("-Djvmguard.saveFlat=true")
            add("-Djvmguard.logUnrecordedPayloads=true")
            add("-Djvmguard.logProbes=0")
            add("-Djvmguard.logCommunication=0")
            add("-Djvmguard.logMBean=0")
            add("-Djvmguard.logInstrumentation=0")
            val initListener = test.isInitListener(1, vmNo, 1)
            if (initListener && test.isWaitForListener(1, vmNo, 1)) {
                add("-Djvmguard.waitForListener=true")
            }
            if (!initListener) {
                add("-Djvmguard.noListener=true")
            }
            add("-Djvmguard.logFile=" + File(workDir, "$vmName.log").absolutePath)
            add("-Djvmguard.int.declaredClass=" + ClassTransaction::class.java.name)
            add("-Djvmguard.int.declaredMethod=" + MethodTransaction::class.java.name)
            add("-Djvmguard.int.declaredNo=" + NoTransaction::class.java.name)
            add("-Djvmguard.int.declaredTelemetry=" + Telemetry::class.java.name)
            add("-D${Util.TEST_CLASS_PROP_NAME}=" + test.getRunClassName())
            add("-D${Util.VMNO_PROP_NAME}=$vmNo")
            add("-D${Util.RUNNO_PROP_NAME}=$runNo")
            add("-D${Util.VM_PROP_NAME}=$vmName")
            add("-classpath")
            add(systemDir("jvmguard.integration.workloadClasspath"))
            add(RunTestAction::class.java.name)
        }

        val consoleLog = File(workDir, "$vmName-run$runNo-console.log")
        return ProcessBuilder(cmd)
            .directory(workDir)
            .redirectErrorStream(true)
            .redirectOutput(consoleLog)
            .start()
    }

    private fun terminateConnections() {
        vmManager.currentConnections.forEach { connection ->
            try {
                vmManager.terminate(connection.vm, true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun destroyProcesses() {
        for (process in processes) {
            if (process.isAlive) {
                process.destroyForcibly()
                try {
                    process.waitFor()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    private class ControllerImpl(override val workingDir: File) : Controller {
        override fun finished() {}
    }

    companion object {
        private const val LOGIN = Credentials.LOGIN
        private const val PASSWORD = Credentials.PASSWORD
        private const val TEST_VM_PORT = "8846"
        private val API_KEY = Credentials.API_KEY

        private fun createInitialConfig(test: JvmGuardTest, dataDir: File): File {
            val configFile = File(dataDir, ImportManager.SERVER_CONFIG_FILE_NAME)

            val globalConfig = GlobalConfig()
            test.modifyInitialGlobalConfig(globalConfig)

            val user = User(LOGIN, "Tester", PasswordHelper.createHash(PASSWORD), "integration_test@test.com", AccessLevel.ADMIN)
            user.apiKeyHash = PasswordHelper.createHash(API_KEY)
            val serverInitConfig = ServerInitConfig(
                globalConfig,
                arrayListOf(user),
                ArrayList(test.getInitialGroupConfigs()),
                ArrayList(),
                ArrayList(),
                ArrayList(),
                ArrayList(),
                ArrayList(),
            )

            configFile.outputStream().buffered().use { serverInitConfig.export(it) }
            return configFile
        }

        private fun seedAdminSecurityContext() {
            val authorities = AccessLevel.entries
                .filter { AccessLevel.ADMIN.isAtLeast(it) }
                .map<AccessLevel, GrantedAuthority> { SimpleGrantedAuthority("ROLE_${it.name}") }
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken.authenticated(LOGIN, null, authorities)
        }

        private fun systemDir(key: String): String =
            System.getProperty(key)
                ?: throw IllegalStateException("system property $key not set by the Gradle integrationTest task")

        private fun deleteRecursively(file: File): Boolean {
            file.listFiles()?.forEach { deleteRecursively(it) }
            return file.delete()
        }
    }
}
