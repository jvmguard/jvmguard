package dev.jvmguard.server

import dev.jvmguard.collector.main.VmManagerImpl
import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.common.JvmGuardDirectories
import dev.jvmguard.common.config.ConfigManager
import dev.jvmguard.common.config.ConfigStorage
import dev.jvmguard.common.helper.GroupHelper
import dev.jvmguard.common.helper.PasswordHelper
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.config.GroupConfig
import dev.jvmguard.data.config.sets.*
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.User
import dev.jvmguard.data.user.UserManager
import dev.jvmguard.ui.server.JvmGuardUserDetails
import dev.jvmguard.connector.api.MockMode
import dev.jvmguard.connector.api.ServerConnectionRegistry
import dev.jvmguard.connector.server.ServerControl
import dev.jvmguard.connector.server.mock.snapshot.SnapshotCapturer
import dev.jvmguard.connector.server.mock.snapshot.SnapshotLoader
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.IOException

@RestController
@RequestMapping(IntegrationTestControlController.PATH)
@Conditional(IntegrationTestControlController.EnabledCondition::class)
class IntegrationTestControlController(
    private val serverControl: ServerControl,
    private val serverConnectionRegistry: ServerConnectionRegistry,
    private val configManager: ConfigManager,
    private val vmManager: VmManagerImpl,
    private val userManager: UserManager,
    private val configStorage: ConfigStorage,
) {

    @Volatile
    private var shutdownReceived = false

    class EnabledCondition : Condition {
        override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
            JvmGuardConfig.isIntegrationTest || java.lang.Boolean.getBoolean("jvmguard.testControlFilter")
    }

    @GetMapping
    @Synchronized
    fun handle(@RequestParam(KEY_COMMAND) command: String, request: HttpServletRequest): ResponseEntity<Void> {
        if (shutdownReceived || serverControl.isShuttingDown) {
            return ResponseEntity.badRequest().build()
        }
        try {
            when (command) {
                COMMAND_SHUTDOWN -> handleShutdown()
                COMMAND_RESET -> handleReset()
                COMMAND_CREATE_USER -> handleCreateUser(request)
                COMMAND_LOG -> handleLog(request)
                COMMAND_DISCONNECT -> handleDisconnect()
                COMMAND_PING -> {}
                COMMAND_CAPTURE_MOCK_SNAPSHOT -> return handleCaptureMockSnapshot(request)
                COMMAND_CHECK_BACKEND_AUTHZ -> return checkBackendAuthz(request)
                else -> return ResponseEntity.badRequest().build()
            }
            return ResponseEntity.accepted().build()
        } catch (e: Exception) {
            ServerMain.LOGGER.warn("Error handling control command {}", command, e)
            return ResponseEntity.badRequest().build()
        }
    }

    private fun handleShutdown() {
        ServerMain.LOGGER.info("Shutdown request received")
        shutdownReceived = true
        Thread(serverControl::shutdown, "test-control-shutdown").apply {
            isDaemon = true
            start()
        }
    }

    private fun handleReset() {
        ServerMain.LOGGER.info("Reset request received")
        serverConnectionRegistry.clearConnections()

        configStorage.removeAll(GroupConfig::class.java)
        configStorage.removeAll(GlobalConfig::class.java)
        configStorage.removeAll(ActionSet::class.java)
        configStorage.removeAll(TelemetrySet::class.java)
        configStorage.removeAll(ThresholdSet::class.java)
        configStorage.removeAll(TransactionDefSet::class.java)
        configStorage.removeAll(TriggerSet::class.java)

        userManager.removeAll()

        for (vm in vmManager.namedVms) {
            vmManager.deleteVM(vm)
        }

        configManager.init()

        assert(serverControl.server.isNewInstallation)
    }

    private fun handleCreateUser(request: HttpServletRequest) {
        val accessLevelName = request.getParameter(PARAMETER_ACCESS_LEVEL)
        if (accessLevelName != null) {
            val user = User().apply {
                loginName = request.getParameter(PARAMETER_NAME)
                passwordHash = PasswordHelper.createHash(request.getParameter(PARAMETER_PASSWORD))
                accessLevel = AccessLevel.valueOf(accessLevelName)
                groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
            }
            val apiKey = request.getParameter(PARAMETER_API_KEY)
            if (apiKey != null) {
                user.apiKeyHash = PasswordHelper.createHash(apiKey)
            }
            userManager.store(user)
            return
        }

        val groupConfig = GroupConfig.createDefault()
        serverControl.server.createInitialUser(
            "test", "Tester", "integration_test@test.com",
            PasswordHelper.createHash("password4329"), false, null, groupConfig,
        )
    }

    private fun checkBackendAuthz(request: HttpServletRequest): ResponseEntity<Void> {
        val accessLevel = AccessLevel.valueOf(request.getParameter(PARAMETER_ACCESS_LEVEL))

        val user = User().apply {
            loginName = "authz-probe"
            this.accessLevel = accessLevel
            groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
        }

        val connection = serverControl.server.connect(user, MockMode.NONE)
        val previousContext = SecurityContextHolder.getContext()
        return try {
            val principal = JvmGuardUserDetails(user.loginName, accessLevel, connection)
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities)
            SecurityContextHolder.setContext(context)

            connection.users
            ResponseEntity.ok().build()
        } catch (_: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        } finally {
            SecurityContextHolder.setContext(previousContext)
            connection.logout()
        }
    }

    private fun handleLog(request: HttpServletRequest) {
        val message = request.getParameter(PARAMETER_MESSAGE)
        var loggerName = request.getParameter(PARAMETER_LOGGER_NAME)
        if (loggerName.isNullOrEmpty()) {
            loggerName = Logger.ROOT_LOGGER_NAME
        }
        LoggerFactory.getLogger(loggerName).info(message)
    }

    private fun handleDisconnect() {
        for (connection in vmManager.currentConnections) {
            try {
                vmManager.terminate(connection.vm, true)
            } catch (e: IOException) {
                ServerMain.LOGGER.warn("Error disconnecting VM", e)
            }
        }
    }

    // driven by the `demoSnapshot` Gradle task, see SnapshotCapturer
    private fun handleCaptureMockSnapshot(request: HttpServletRequest): ResponseEntity<Void> {
        val admin = serverControl.server.authenticate("test", "password4329", null)
        val connection = serverControl.server.connect(admin, MockMode.NONE)
        try {
            val snapshot = SnapshotCapturer(connection).capture()
            val dataDirectory = ServerMain.getBean(JvmGuardDirectories::class.java).dataDirectory
            val path = request.getParameter("path") ?: File(dataDirectory, "demo-snapshot.json.gz").absolutePath
            SnapshotLoader.write(snapshot, File(path))
            ServerMain.LOGGER.info("Wrote demo snapshot to {} ({} VMs, anchor={})",
                path, snapshot.vms.size, snapshot.captureAnchor)
        } finally {
            connection.logout()
        }
        return ResponseEntity.ok().build()
    }

    companion object {
        const val PATH = "/test"

        const val KEY_COMMAND = "command"
        const val COMMAND_SHUTDOWN = "shutdown"
        const val COMMAND_RESET = "reset"
        const val COMMAND_CREATE_USER = "createUser"
        const val COMMAND_LOG = "log"
        const val PARAMETER_MESSAGE = "message"
        const val PARAMETER_LOGGER_NAME = "loggerName"
        const val PARAMETER_ACCESS_LEVEL = "accessLevel"
        const val PARAMETER_NAME = "name"
        const val PARAMETER_PASSWORD = "password"
        const val PARAMETER_API_KEY = "apiKey"
        const val COMMAND_DISCONNECT = "disconnect"
        const val COMMAND_PING = "ping"
        const val COMMAND_CAPTURE_MOCK_SNAPSHOT = "captureMockSnapshot"
        const val COMMAND_CHECK_BACKEND_AUTHZ = "checkBackendAuthz"
    }
}
