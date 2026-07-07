package com.jvmguard.server.sso

import com.fasterxml.jackson.databind.ObjectMapper
import com.jvmguard.data.config.SsoGroupMapping
import com.jvmguard.data.config.SsoPreset
import com.jvmguard.data.user.AccessLevel
import com.nimbusds.jwt.JWTParser
import dasniko.testcontainers.keycloak.KeycloakContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Integration test against a  Keycloak OIDC provider via Testcontainers.
 * Uses the Direct Access Grants flow to obtain tokens without a browser redirect
 *
 * The realm JSON at keycloak-test-realm.json contains:
 * - Client: "jvmguard", secret "test-secret-12345" with a group membership mapper emitting a "groups" claim
 * - User "alice@example.com", password "alice" in group "jvmguard-admins"
 * - User "bob@example.com", password "bob" in group "jvmguard-viewers"
 *
 * For manual browser testing of the full OIDC redirect (run from the project root):
 * ```
 * docker run --rm -p 8180:8080 \
 *   -v "$PWD/modules/server/src/test/resources/keycloak-test-realm.json:/opt/keycloak/data/import/realm.json:ro" \
 *   quay.io/keycloak/keycloak:26.0 start-dev --import-realm
 * ```
 *
 * Then in jvmguard: Settings -> Single Sign-On -> Add provider:
 * - Display name: Keycloak
 * - Provider type: "Keycloak"
 * - Issuer URI: http://localhost:8180/realms/jvmguard
 * - Client ID: jvmguard
 * - Client secret: test-secret-12345
 * - Domain: example.com
 * - Access rules: jvmguard-admins -> Admin, jvmguard-viewers -> Viewer
 */
class KeycloakSsoGroupTest : BaseSsoTest() {

    private val issuer: String
        get() = keycloak?.authServerUrl?.trimEnd('/')?.let { "$it/realms/jvmguard" } ?: error("Keycloak not started")

    private fun rule(claimValue: String, level: AccessLevel) =
        SsoGroupMapping().apply { this.claimValue = claimValue; this.accessLevel = level }

    @Test
    fun adminGroupUserGetsAdminRole() {
        configureProvider(
            issuer, domainRestriction = "example.com", preset = SsoPreset.GENERIC_OIDC,
            accessRules = listOf(
                rule("jvmguard-admins", AccessLevel.ADMIN),
                rule("jvmguard-viewers", AccessLevel.VIEWER),
            )
        )

        val token = passwordGrant("alice", "alice")
        val user = server.authenticateSso(issuer, token.subject, token.email, token.name, token.groups)

        assertEquals("alice@example.com", user.loginName)
        assertEquals(AccessLevel.ADMIN, user.accessLevel, "alice is in jvmguard-admins -> ADMIN")
    }

    @Test
    fun viewerGroupUserGetsViewerRole() {
        configureProvider(
            issuer, domainRestriction = "example.com", preset = SsoPreset.GENERIC_OIDC,
            accessRules = listOf(
                rule("jvmguard-admins", AccessLevel.ADMIN),
                rule("jvmguard-viewers", AccessLevel.VIEWER),
            )
        )

        val token = passwordGrant("bob", "bob")
        val user = server.authenticateSso(issuer, token.subject, token.email, token.name, token.groups)

        assertEquals(AccessLevel.VIEWER, user.accessLevel, "bob is in jvmguard-viewers -> VIEWER")
    }

    @Test
    fun catchAllGivesDefaultRoleWhenGroupDoesNotMatch() {
        configureProvider(
            issuer, domainRestriction = "example.com", preset = SsoPreset.GENERIC_OIDC,
            accessRules = listOf(
                rule("jvmguard-admins", AccessLevel.ADMIN),
                rule(SsoGroupMapping.CATCH_ALL, AccessLevel.VIEWER),
            )
        )

        val token = passwordGrant("bob", "bob")
        val user = server.authenticateSso(issuer, token.subject, token.email, token.name, token.groups)

        assertEquals(AccessLevel.VIEWER, user.accessLevel, "bob's group doesn't match a specific rule, catch-all -> VIEWER")
    }

    private fun passwordGrant(username: String, password: String): TokenClaims {
        val client = HttpClient.newHttpClient()
        val body = "client_id=jvmguard&client_secret=test-secret-12345&grant_type=password" +
                "&username=$username&password=$password&scope=openid+profile+email"
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$issuer/protocol/openid-connect/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode(), "Token request failed: ${response.body()}")

        val json = ObjectMapper().readTree(response.body())
        val idToken = json.get("id_token").asText()
        val claims = JWTParser.parse(idToken).jwtClaimsSet

        val groups = (claims.getClaim("groups") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        return TokenClaims(
            subject = claims.subject,
            email = claims.getClaim("email")?.toString() ?: "",
            name = claims.getClaim("name")?.toString(),
            groups = groups,
        )
    }

    private data class TokenClaims(val subject: String, val email: String, val name: String?, val groups: List<String>)

    companion object {
        private var keycloak: KeycloakContainer? = null

        @BeforeAll
        @JvmStatic
        fun startKeycloak() {
            try {
                keycloak = KeycloakContainer("quay.io/keycloak/keycloak:26.0")
                    .withRealmImportFile("keycloak-test-realm.json")
                keycloak!!.start()
            } catch (e: Exception) {
                Assumptions.assumeTrue(false, "Docker not available, skipping Keycloak SSO test: ${e.message}")
            }
        }

        @AfterAll
        @JvmStatic
        fun stopKeycloak() {
            keycloak?.stop()
        }
    }
}
