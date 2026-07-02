package com.jvmguard.build

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.json.JSONObject

enum class Secret {
    NOTARIZATION_KEY,
    MAVEN_CENTRAL,
    INSTALL4J_LICENSE_KEY,
    GITHUB_TOKEN;

    private val cachedValueDelegate = lazy {
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Secret $name is not available")
    }

    val value: String by cachedValueDelegate

    val hasValue: Boolean
        get() = cachedValueDelegate.isInitialized() || !System.getenv(name).isNullOrBlank()

    fun getComponent(key: String): String = JSONObject(value).getString(key)

    fun resolveComponentInConfigurationPhase(key: String, providers: ProviderFactory, optional: Boolean = false): String = providers.of(SecretValueSource::class.java) {
        parameters.secretName.set(name)
        parameters.componentKey.set(key)
        parameters.optional.set(optional)
    }.get()

}

abstract class SecretValueSource : ValueSource<String, SecretValueSource.Params> {
    interface Params : ValueSourceParameters {
        val secretName: Property<String>
        val componentKey: Property<String>
        val optional: Property<Boolean>
    }

    override fun obtain(): String {
        val secret = Secret.valueOf(parameters.secretName.get())
        return try {
            parameters.componentKey.orNull?.let { secret.getComponent(it) } ?: secret.value
        } catch (e: Exception) {
            if (parameters.optional.orNull == true) {
                ""
            } else {
                throw e
            }
        }
    }
}
