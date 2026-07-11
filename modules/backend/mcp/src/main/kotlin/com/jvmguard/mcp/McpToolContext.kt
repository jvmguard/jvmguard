package com.jvmguard.mcp

import com.jvmguard.common.config.ConfigManager
import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.connector.api.MockMode
import com.jvmguard.connector.api.Server
import com.jvmguard.connector.api.ServerConnection
import com.jvmguard.data.config.GuardrailConfig
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.data.user.User
import com.jvmguard.data.user.UserManager
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

    fun guardrails(): GuardrailConfig = configManager.getGlobalConfig(false).guardrailConfig

    fun cappedRecordingSeconds(requestedSeconds: Int): Int {
        val max = guardrails().maxRecordingSeconds
        return if (max > 0) minOf(requestedSeconds, max) else requestedSeconds
    }

    fun requireCaptureAllowed(type: SnapshotFileType, vmPath: String) {
        val allowed = when (type) {
            SnapshotFileType.HPZ -> guardrails().allowHeapDump
            SnapshotFileType.JPS -> guardrails().allowJps
            SnapshotFileType.JFR -> guardrails().allowJfr
            else -> true
        }
        if (!allowed) {
            throw GuardrailException("$type captures are disabled by an administrator.")
        }
        requireCaptureCooldown(vmPath)
    }

    fun requireCaptureCooldown(vmPath: String) {
        val cooldownSeconds = guardrails().captureCooldownSeconds
        val within = captureRateLimiter.secondsSinceLastWithinCooldown(vmPath, cooldownSeconds)
        if (within != null) {
            throw GuardrailException(
                "A capture on \"$vmPath\" was taken ${within}s ago; the minimum interval is " +
                        "${cooldownSeconds}s. Retry later.",
            )
        }
    }

    fun recordCapture(vmPath: String) = captureRateLimiter.recordCapture(vmPath)

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
