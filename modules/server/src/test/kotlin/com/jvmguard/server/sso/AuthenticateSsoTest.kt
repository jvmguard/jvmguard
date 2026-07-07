package com.jvmguard.server.sso

import com.jvmguard.common.helper.GroupHelper
import com.jvmguard.data.config.SsoGroupMapping
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.User
import com.jvmguard.data.user.UserType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AuthenticateSsoTest : BaseSsoTest() {

    private val issuer = "https://accounts.example.com"

    private fun rule(claimValue: String, level: AccessLevel) =
        SsoGroupMapping().apply { this.claimValue = claimValue; this.accessLevel = level }

    @Test
    fun existingUserBySubjectReturnsImmediately() {
        configureProvider(
            issuer, domainRestriction = "example.com",
            accessRules = listOf(rule("*", AccessLevel.VIEWER))
        )

        val user = User().apply {
            loginName = "alice@example.com"
            userType = UserType.OIDC
            ssoIssuer = issuer
            ssoSubject = "sub-123"
            email = "alice@example.com"
            accessLevel = AccessLevel.ADMIN
            groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
        }
        userManager.store(user)

        val result = server.authenticateSso(issuer, "sub-123", "alice@example.com", null, emptyList())
        assertEquals("alice@example.com", result.loginName)
        assertEquals(AccessLevel.ADMIN, result.accessLevel, "admin override on the row is respected")
    }

    @Test
    fun existingUserByEmailPinsSubject() {
        configureProvider(
            issuer, domainRestriction = "example.com",
            accessRules = listOf(rule("*", AccessLevel.VIEWER))
        )

        val user = User().apply {
            loginName = "bob@example.com"
            userType = UserType.OIDC
            ssoIssuer = issuer
            ssoSubject = ""
            email = "bob@example.com"
            accessLevel = AccessLevel.PROFILER
            groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
        }
        userManager.store(user)

        val result = server.authenticateSso(issuer, "sub-456", "bob@example.com", null, emptyList())
        assertEquals("bob@example.com", result.loginName)
        assertEquals(AccessLevel.PROFILER, result.accessLevel)

        val stored = userManager.getByLoginName("bob@example.com")!!
        assertEquals("sub-456", stored.ssoSubject, "subject was pinned")
    }

    @Test
    fun noUserWithGroupMatchAutoProvisions() {
        configureProvider(
            issuer, domainRestriction = "example.com",
            accessRules = listOf(
                rule("admins", AccessLevel.ADMIN),
                rule("*", AccessLevel.VIEWER),
            )
        )

        val result = server.authenticateSso(issuer, "sub-789", "carol@example.com", null, listOf("admins"))
        assertEquals("carol@example.com", result.loginName)
        assertEquals(AccessLevel.ADMIN, result.accessLevel, "matched the 'admins' group rule")
        assertEquals(UserType.OIDC, result.userType)

        val stored = userManager.getByLoginName("carol@example.com")
        assertNotNull(stored, "user was auto-provisioned")
        assertEquals("sub-789", stored!!.ssoSubject)
    }

    @Test
    fun noUserWithCatchAllAutoProvisions() {
        configureProvider(
            issuer, domainRestriction = "example.com",
            accessRules = listOf(rule("*", AccessLevel.VIEWER))
        )

        val result = server.authenticateSso(issuer, "sub-000", "dave@example.com", null, emptyList())
        assertEquals(AccessLevel.VIEWER, result.accessLevel, "matched the catch-all")
    }

    @Test
    fun noUserWithEmptyRulesIsDenied() {
        configureProvider(issuer, domainRestriction = "example.com")

        assertThrows<javax.security.auth.login.FailedLoginException> {
            server.authenticateSso(issuer, "sub-aaa", "eve@example.com", null, listOf("admins"))
        }
    }

    @Test
    fun noUserWithRulesButNoMatchIsDenied() {
        configureProvider(
            issuer, domainRestriction = "example.com",
            accessRules = listOf(rule("admins", AccessLevel.ADMIN))
        )

        assertThrows<javax.security.auth.login.FailedLoginException> {
            server.authenticateSso(issuer, "sub-bbb", "frank@example.com", null, listOf("developers"))
        }
    }

    @Test
    fun domainRestrictionMismatchIsDenied() {
        configureProvider(issuer, domainRestriction = "example.com")

        assertThrows<javax.security.auth.login.FailedLoginException> {
            server.authenticateSso(issuer, "sub-ccc", "intruder@evil.com", null, listOf("admins"))
        }
    }

    @Test
    fun specificGroupWinsOverCatchAll() {
        configureProvider(
            issuer, domainRestriction = "example.com",
            accessRules = listOf(
                rule("viewers", AccessLevel.VIEWER),
                rule("admins", AccessLevel.ADMIN),
                rule("*", AccessLevel.VIEWER),
            )
        )

        val result = server.authenticateSso(issuer, "sub-ddd", "grace@example.com", null, listOf("admins"))
        assertEquals(AccessLevel.ADMIN, result.accessLevel, "specific 'admins' rule wins over catch-all")
    }

    @Test
    fun unconfiguredIssuerIsDenied() {
        assertThrows<javax.security.auth.login.FailedLoginException> {
            server.authenticateSso("https://unknown-issuer.com", "sub-eee", "user@example.com", null, emptyList())
        }
    }

    @Test
    fun issuerTrailingSlashIsMatched() {
        // Auth0 reports its issuer with a trailing slash in discovery metadata, while the configured
        // provider value usually omits it. The provider lookup must match regardless of the slash.
        configureProvider(
            issuer, domainRestriction = "example.com",
            accessRules = listOf(rule("*", AccessLevel.VIEWER))
        )

        val result = server.authenticateSso("$issuer/", "sub-slash", "heidi@example.com", null, emptyList())
        assertEquals("heidi@example.com", result.loginName)
        assertEquals(AccessLevel.VIEWER, result.accessLevel, "trailing-slash issuer still resolves the provider")
    }
}
