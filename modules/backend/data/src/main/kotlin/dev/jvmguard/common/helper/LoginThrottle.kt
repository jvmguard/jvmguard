package dev.jvmguard.common.helper

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class LoginThrottle {

    private class State {
        var failures: Int = 0
        var notBefore: Long = 0L
    }

    private val states = ConcurrentHashMap<String, State>()

    fun isThrottled(loginName: String): Boolean {
        val state = states[loginName] ?: return false
        return state.failures >= FREE_ATTEMPTS && System.currentTimeMillis() < state.notBefore
    }

    fun loginFailed(loginName: String) {
        if (states.size > MAX_ENTRIES) {
            states.clear()
        }
        val state = states.getOrPut(loginName) { State() }
        synchronized(state) {
            state.failures++
            val delaySeconds = minOf(BASE_DELAY_SECONDS shl (state.failures - 1).coerceAtMost(MAX_SHIFT), MAX_DELAY_SECONDS)
            state.notBefore = System.currentTimeMillis() + delaySeconds * 1000L
        }
    }

    fun loginSucceeded(loginName: String) {
        states.remove(loginName)
    }

    companion object {
        private const val FREE_ATTEMPTS = 3
        private const val BASE_DELAY_SECONDS = 2
        private const val MAX_DELAY_SECONDS = 300
        private const val MAX_SHIFT = 8
        private const val MAX_ENTRIES = 10000
    }
}
