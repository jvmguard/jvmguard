package com.jvmguard.server.sso

import com.jvmguard.common.Loggers
import com.jvmguard.common.config.ConfigChangeListener
import com.jvmguard.common.config.ConfigManager
import com.jvmguard.connector.api.SsoState
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.config.SsoPreset
import com.jvmguard.data.config.SsoProviderConfig
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ClientRegistrations
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.stereotype.Component

@Primary
@Component
class MutableClientRegistrationRepository(
    private val configManager: ConfigManager,
) : ClientRegistrationRepository, ConfigChangeListener, Iterable<ClientRegistration> {

    @Volatile
    private var registrations: Map<String, ClientRegistration> = emptyMap()

    init {
        configManager.addConfigChangeListener(this)
        refresh()
    }

    override fun findByRegistrationId(registrationId: String): ClientRegistration? =
        registrations[registrationId]

    override fun iterator(): Iterator<ClientRegistration> = registrations.values.iterator()

    fun refresh() {
        val config = configManager.getGlobalConfig(false)
        val newRegistrations = LinkedHashMap<String, ClientRegistration>()
        for (provider in config.ssoConfig.providers.filter { it.enabled }) {
            val needsIssuer = provider.preset != SsoPreset.GOOGLE_WORKSPACE
            if (provider.effectiveClientId().isBlank() || (needsIssuer && provider.issuerUri.isBlank())) {
                continue
            }
            try {
                val reg = buildRegistration(provider)
                newRegistrations[reg.registrationId] = reg
            } catch (e: Exception) {
                Loggers.SERVER.warn(
                    "Failed to build OIDC client registration for '{}': {}",
                    provider.displayName,
                    e.message,
                )
            }
        }
        registrations = newRegistrations
        SsoState.activeSlugs = newRegistrations.keys
        Loggers.SERVER.info("SSO client registrations refreshed: {} provider(s) active: {}",
            newRegistrations.size, newRegistrations.keys)
    }

    override fun globalConfigChanged(oldConfig: GlobalConfig?, newConfig: GlobalConfig) {
        Thread.startVirtualThread { refresh() }
    }

    override fun groupConfigsChanged() {}

    private fun buildRegistration(provider: SsoProviderConfig): ClientRegistration {
        val registrationId = SsoProviderConfig.slugify(provider.displayName)
        val issuerUri = provider.issuerUri.trim()
        val scopes = provider.scopes.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val builder = when (provider.preset) {
            SsoPreset.GOOGLE_WORKSPACE -> ClientRegistration.withRegistrationId(registrationId)
                .clientId(provider.effectiveClientId().trim())
                .clientSecret(provider.effectiveClientSecret().trim())
                .clientName(provider.displayName)
                .issuerUri("https://accounts.google.com")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName(provider.userNameAttribute)

            else -> {
                val normalized = issuerUri.trimEnd('/')
                val discoveryBuilder = try {
                    ClientRegistrations.fromIssuerLocation(normalized)
                } catch (_: Exception) {
                    ClientRegistrations.fromIssuerLocation("$normalized/")
                }
                discoveryBuilder
                    .registrationId(registrationId)
                    .clientId(provider.effectiveClientId().trim())
                    .clientSecret(provider.effectiveClientSecret().trim())
                    .clientName(provider.displayName)
                    .userNameAttributeName(provider.userNameAttribute)
            }
        }

        if (scopes.isNotEmpty()) {
            builder.scope(*scopes.toTypedArray())
        }

        return builder.build()
    }
}
