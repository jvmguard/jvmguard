package com.jvmguard.mcp.auth

import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.UserManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves an MCP API key presented as an "Authorization: Bearer <key>" token to the owning
 * user
 */
class ApiKeyBearerAuthenticationFilter(
    private val userManager: UserManager,
) : OncePerRequestFilter() {

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val positiveTtlMillis: Long = 5 * 60 * 1000L
    private val negativeTtlMillis: Long = 30 * 1000L
    private val maxCacheEntries = 10_000

    // loginName is null for a cached negative
    private data class CacheEntry(val loginName: String?, val expiresAt: Long)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val bearerToken = extractBearerToken(request)
        if (bearerToken != null) {
            resolveApiKey(bearerToken)?.let { SecurityContextHolder.getContext().authentication = it }
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveApiKey(apiKey: String): Authentication? {
        val cacheKey = sha256(apiKey)
        val now = System.currentTimeMillis()

        val cached = cache[cacheKey]
        if (cached != null && cached.expiresAt > now) {
            val loginName = cached.loginName ?: return null
            return userManager.getByLoginName(loginName)?.let { authenticationFor(it.loginName, it.accessLevel) }
        }

        for (user in userManager.getAllUsers()) {
            if (user.apiKeyHash.isNotEmpty() && PasswordHelper.validatePassword(apiKey, user.apiKeyHash)) {
                store(cacheKey, CacheEntry(user.loginName, now + positiveTtlMillis))
                return authenticationFor(user.loginName, user.accessLevel)
            }
        }
        store(cacheKey, CacheEntry(null, now + negativeTtlMillis))
        return null
    }

    private fun authenticationFor(loginName: String, accessLevel: AccessLevel) =
        UsernamePasswordAuthenticationToken.authenticated(loginName, null, McpAuthorities.forAccessLevel(accessLevel))

    private fun store(cacheKey: String, entry: CacheEntry) {
        // a flood of distinct invalid tokens must not grow the cache without limit
        if (cache.size >= maxCacheEntries) {
            cache.clear()
        }
        cache[cacheKey] = entry
    }

    private fun extractBearerToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
