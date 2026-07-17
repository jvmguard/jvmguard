package dev.jvmguard.common.helper

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginThrottleTest {

    @Test
    fun firstFailuresAreNotThrottled() {
        val throttle = LoginThrottle()
        repeat(2) { throttle.loginFailed("user") }
        assertFalse(throttle.isThrottled("user"))
    }

    @Test
    fun repeatedFailuresTriggerThrottling() {
        val throttle = LoginThrottle()
        repeat(3) { throttle.loginFailed("user") }
        assertTrue(throttle.isThrottled("user"))
    }

    @Test
    fun otherUsersAreNotAffected() {
        val throttle = LoginThrottle()
        repeat(3) { throttle.loginFailed("user") }
        assertFalse(throttle.isThrottled("other"))
    }

    @Test
    fun successClearsFailures() {
        val throttle = LoginThrottle()
        repeat(2) { throttle.loginFailed("user") }
        throttle.loginSucceeded("user")
        repeat(2) { throttle.loginFailed("user") }
        assertFalse(throttle.isThrottled("user"))
    }

    @Test
    fun unknownUserIsNotThrottled() {
        assertFalse(LoginThrottle().isThrottled("nobody"))
    }
}
