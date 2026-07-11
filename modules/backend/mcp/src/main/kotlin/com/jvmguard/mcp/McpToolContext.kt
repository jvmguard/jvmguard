package com.jvmguard.mcp

import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.connector.api.MockMode
import com.jvmguard.connector.api.Server
import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.data.config.GuardrailConfig
import com.jvmguard.data.config.guardrails.GuardrailSettings
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.data.user.User
import com.jvmguard.data.user.UserManager
import com.jvmguard.data.vmdata.VM
import com.jvmguard.mcp.auth.McpAuthorities
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

@Component
class McpToolContext(
    private val server: Server,
    private val userManager: UserManager,
    private val downloadTokens: McpDownloadTokens,
    private val configManager: ConfigManager,
    private val captureRateLimiter: CaptureRateLimiter,
) {

    companion object {
        val authTokenHolder: ThreadLocal<String> = ThreadLocal()
        val baseUrlHolder: ThreadLocal<String> = ThreadLocal()
        val clientIpHolder: ThreadLocal<String> = ThreadLocal()

        private const val MAX_CACHE_ENTRIES = 10_000
    }

    // Server-wide guardrails (MCP read-only switch, IP allowlist). Per-VM-group capture/action toggles live
    // in the group's GuardrailSettings and are resolved with guardrailsFor(vm).
    fun globalGuardrails(): GuardrailConfig = configManager.getGlobalConfig(false).guardrailConfig

    // Effective per-group guardrails for the VM, inheriting with override semantics up the group hierarchy.
    fun guardrailsFor(vm: VM): GuardrailSettings = configManager.getGroupHierarchyWrapper(vm).guardrailSettings

    fun cappedRecordingSeconds(vm: VM, requestedSeconds: Int): Int {
        val max = guardrailsFor(vm).maxRecordingSeconds
        return if (max > 0) minOf(requestedSeconds, max) else requestedSeconds
    }

    fun requireCaptureAllowed(type: SnapshotFileType, vm: VM) {
        val guardrails = guardrailsFor(vm)
        val allowed = when (type) {
            SnapshotFileType.HPZ -> guardrails.allowHeapDump
            SnapshotFileType.JPS -> guardrails.allowJps
            SnapshotFileType.JFR -> guardrails.allowJfr
            else -> true
        }
        if (!allowed) {
            throw GuardrailException("$type captures are disabled for \"${vm.hierarchyPath}\".")
        }
        requireCaptureCooldown(vm)
    }

    fun requireCaptureCooldown(vm: VM) {
        val cooldownSeconds = guardrailsFor(vm).captureCooldownSeconds
        val within = captureRateLimiter.secondsSinceLastWithinCooldown(vm.hierarchyPath, cooldownSeconds)
        if (within != null) {
            throw GuardrailException(
                "A capture on \"${vm.hierarchyPath}\" was taken ${within}s ago; the minimum interval is " +
                        "${cooldownSeconds}s. Retry later.",
            )
        }
    }

    fun requireRunGcAllowed(vm: VM) {
        if (!guardrailsFor(vm).allowRunGc) {
            throw GuardrailException("Forced garbage collection is disabled for \"${vm.hierarchyPath}\".")
        }
    }

    fun requireMbeanMutationAllowed(vm: VM) {
        if (!guardrailsFor(vm).allowMbeanMutations) {
            throw GuardrailException(
                "Setting MBean attributes and invoking MBean operations is disabled for \"${vm.hierarchyPath}\".",
            )
        }
    }

    fun recordCapture(vm: VM) = captureRateLimiter.recordCapture(vm.hierarchyPath)

    fun currentBaseUrl(): String? = baseUrlHolder.get()?.takeIf { it.isNotBlank() }

    fun currentClientIp(): String? = clientIpHolder.get()?.takeIf { it.isNotBlank() }

    // must not read the SecurityContext
    fun currentPrincipal(): String? {
        val token = authTokenHolder.get()?.removePrefix("Bearer ")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return resolveUser(token)?.loginName
    }

    fun createDownloadToken(fileId: Long): String {
        val loginName = SecurityContextHolder.getContext().authentication?.name
            ?: throw McpError("No authenticated user in context")
        return downloadTokens.createToken(fileId, loginName)
    }

    private val apiKeyCache = ConcurrentHashMap<String, String>()

    fun <R> withConnection(block: (ServerConnection) -> R): R {
        val authHeader = authTokenHolder.get() ?: throw McpError("No auth token in context")
        val token = authHeader.removePrefix("Bearer ").trim()
        if (token.isEmpty()) throw McpError("Missing authentication token")

        val user = resolveUser(token) ?: throw McpError("Invalid authentication token")
        val authorities = McpAuthorities.forAccessLevel(user.accessLevel)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken.authenticated(user.loginName, null, authorities)

        val connection = server.connect(user, MockMode.NONE)
        return try {
            block(connection)
        } finally {
            connection.logout()
            SecurityContextHolder.clearContext()
        }
    }

    // Opens a connection for an authenticated login name
    fun <R> withConnectionForPrincipal(loginName: String, block: (ServerConnection) -> R): R {
        val user = userManager.getByLoginName(loginName) ?: throw McpError("Unknown user: $loginName")
        val connection = server.connect(user, MockMode.NONE)
        return try {
            block(connection)
        } finally {
            connection.logout()
        }
    }

    private fun resolveUser(token: String): User? = resolveApiKeyUser(token)

    private fun resolveApiKeyUser(apiKey: String): User? {
        val cacheKey = sha256(apiKey)
        apiKeyCache[cacheKey]?.let { return userManager.getByLoginName(it) }
        for (user in userManager.getAllUsers()) {
            if (user.apiKeyHash.isNotEmpty() && PasswordHelper.validatePassword(apiKey, user.apiKeyHash)) {
                if (apiKeyCache.size >= MAX_CACHE_ENTRIES) apiKeyCache.clear()
                apiKeyCache[cacheKey] = user.loginName
                return userManager.getByLoginName(user.loginName)
            }
        }
        return null
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
