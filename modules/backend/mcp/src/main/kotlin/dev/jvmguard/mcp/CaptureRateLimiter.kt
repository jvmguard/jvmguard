package dev.jvmguard.mcp

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class CaptureRateLimiter {

    private val lastCaptureMillis = ConcurrentHashMap<String, Long>()

    /**
     * Seconds since the last capture on vmPath if it is still within cooldownSeconds, or null when a
     * capture is allowed
     */
    fun secondsSinceLastWithinCooldown(vmPath: String, cooldownSeconds: Int): Long? {
        if (cooldownSeconds <= 0) {
            return null
        }
        val last = lastCaptureMillis[key(vmPath)] ?: return null
        val elapsedSeconds = (System.currentTimeMillis() - last) / 1000
        return if (elapsedSeconds < cooldownSeconds) elapsedSeconds else null
    }

    fun recordCapture(vmPath: String) {
        lastCaptureMillis[key(vmPath)] = System.currentTimeMillis()
    }

    private fun key(vmPath: String): String = vmPath.trimEnd('/')
}
