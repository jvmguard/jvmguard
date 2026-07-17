package dev.jvmguard.server

import dev.jvmguard.common.export.TelemetryExport
import dev.jvmguard.common.export.TransactionTreeExport
import dev.jvmguard.common.helper.LoginThrottle
import dev.jvmguard.data.transactions.TransactionTreeInterval
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.rest.entity.GroupEntity
import dev.jvmguard.rest.entity.TelemetryDescriptor
import dev.jvmguard.rest.restInterface.RestInterface
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

class RestApiKeyAuthenticationProviderTest {

    private class FakeRestInterface(private val validKey: String) : RestInterface {
        override fun checkAccess(name: String, apiKey: String): AccessLevel? =
            if (apiKey == validKey) AccessLevel.ADMIN else null

        override fun getGroups(): List<GroupEntity> = throw UnsupportedOperationException()
        override fun getVms(groupName: String?, connected: Boolean): List<String> = throw UnsupportedOperationException()
        override fun getTelemetryDescriptors(): List<TelemetryDescriptor> = throw UnsupportedOperationException()
        override fun getTelemetry(
            vmName: String?,
            groupName: String?,
            telemetryName: String,
            telemetryInterval: TelemetryInterval,
            endTime: Long
        ): TelemetryExport = throw UnsupportedOperationException()

        override fun getCallTree(
            vmName: String?,
            groupName: String?,
            interval: TransactionTreeInterval,
            startTime: Long,
            mergePolicies: Boolean
        ): TransactionTreeExport = throw UnsupportedOperationException()

        override fun getHotSpots(
            vmName: String?,
            groupName: String?,
            interval: TransactionTreeInterval,
            startTime: Long,
            mergePolicies: Boolean
        ): TransactionTreeExport = throw UnsupportedOperationException()

        override fun getOverdue(
            vmName: String?,
            groupName: String?,
            interval: TransactionTreeInterval,
            startTime: Long
        ): TransactionTreeExport = throw UnsupportedOperationException()

        override fun triggerBackup() = throw UnsupportedOperationException()
    }

    @Test
    fun validKeyAuthenticates() {
        val provider = RestApiKeyAuthenticationProvider(FakeRestInterface("secret"), LoginThrottle())
        val result = provider.authenticate(UsernamePasswordAuthenticationToken("user", "secret"))
        assertEquals("user", result.name)
    }

    @Test
    fun invalidKeyIsRejectedWithoutSleeping() {
        val provider = RestApiKeyAuthenticationProvider(FakeRestInterface("secret"), LoginThrottle())
        val start = System.nanoTime()
        assertThrows<BadCredentialsException> {
            provider.authenticate(UsernamePasswordAuthenticationToken("user", "wrong"))
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue(elapsedMs < 1000, "failed authentication must not block the worker thread, took ${elapsedMs}ms")
    }

    @Test
    fun repeatedFailuresAreThrottled() {
        val throttle = LoginThrottle()
        val provider = RestApiKeyAuthenticationProvider(FakeRestInterface("secret"), throttle)
        repeat(3) {
            assertThrows<BadCredentialsException> {
                provider.authenticate(UsernamePasswordAuthenticationToken("user", "wrong"))
            }
        }
        val e = assertThrows<BadCredentialsException> {
            provider.authenticate(UsernamePasswordAuthenticationToken("user", "secret"))
        }
        assertEquals("Too many failed login attempts", e.message)
    }

    @Test
    fun successResetsThrottling() {
        val provider = RestApiKeyAuthenticationProvider(FakeRestInterface("secret"), LoginThrottle())
        repeat(2) {
            assertThrows<BadCredentialsException> {
                provider.authenticate(UsernamePasswordAuthenticationToken("user", "wrong"))
            }
        }
        provider.authenticate(UsernamePasswordAuthenticationToken("user", "secret"))
        repeat(2) {
            assertThrows<BadCredentialsException> {
                provider.authenticate(UsernamePasswordAuthenticationToken("user", "wrong"))
            }
        }
        provider.authenticate(UsernamePasswordAuthenticationToken("user", "secret"))
    }
}
