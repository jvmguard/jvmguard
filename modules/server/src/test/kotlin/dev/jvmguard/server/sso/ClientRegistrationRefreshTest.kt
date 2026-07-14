package dev.jvmguard.server.sso

import dev.jvmguard.common.config.ConfigManager
import dev.jvmguard.data.config.SsoPreset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClientRegistrationRefreshTest : BaseSsoTest() {

    @Test
    fun googleWorkspaceIssuerIsNormalizedAndPersisted() {
        configureProvider(issuer = "", preset = SsoPreset.GOOGLE_WORKSPACE)

        // The canonical issuer is filled in on save, so the "in use" user count can match on it.
        assertEquals(
            "https://accounts.google.com",
            configManager.getGlobalConfig(false).ssoConfig.providers.first().issuerUri,
        )

        // It is persisted, not merely resolved in memory: a fresh ConfigManager over the same storage sees it too.
        val reloaded = ConfigManager(configStorage)
        assertEquals(
            "https://accounts.google.com",
            reloaded.getGlobalConfig(false).ssoConfig.providers.first().issuerUri,
        )
    }

    @Test
    fun googleWorkspaceProviderActivatesWithoutAnIssuerUri() {
        // The Google preset uses fixed endpoints and leaves the issuer URI blank; it must still activate.
        configureProvider(issuer = "", preset = SsoPreset.GOOGLE_WORKSPACE)

        val registrations = MutableClientRegistrationRepository(configManager).toList()

        assertEquals(1, registrations.size, "the Google provider must be active despite a blank issuer URI")
        assertEquals("https://accounts.google.com", registrations.first().providerDetails.issuerUri)
    }

    @Test
    fun genericProviderWithoutAnIssuerUriStaysInactive() {
        configureProvider(issuer = "", preset = SsoPreset.GENERIC_OIDC)

        assertTrue(
            MutableClientRegistrationRepository(configManager).toList().isEmpty(),
            "a generic provider cannot resolve its endpoints without an issuer URI",
        )
    }
}
