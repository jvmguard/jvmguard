package com.jvmguard.mcp

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Short-lived, single-use tokens that let an MCP client download a snapshot artifact with a plain HTTP GET.
 * A token is scoped to a single file id and the login name of the user who requested it.
 */
@Component
class McpDownloadTokens {

    private data class Entry(val fileId: Long, val loginName: String, val expiresAt: Long)

    private val random = SecureRandom()
    private val tokens = ConcurrentHashMap<String, Entry>()
    private val ttlMillis = 5 * 60 * 1000L
    private val maxEntries = 10_000

    fun createToken(fileId: Long, loginName: String): String {
        purgeIfLarge()
        val token = newToken()
        tokens[token] = Entry(fileId, loginName, System.currentTimeMillis() + ttlMillis)
        return token
    }

    fun consume(token: String, fileId: Long): String? {
        val entry = tokens.remove(token) ?: return null
        if (entry.fileId != fileId || entry.expiresAt < System.currentTimeMillis()) return null
        return entry.loginName
    }

    private fun purgeIfLarge() {
        if (tokens.size < maxEntries) return
        val now = System.currentTimeMillis()
        tokens.entries.removeIf { it.value.expiresAt < now }
        if (tokens.size >= maxEntries) tokens.clear()
    }

    private fun newToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
