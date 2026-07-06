package com.jvmguard.data.config

enum class SsoPreset(val supportsGroups: Boolean, private val verbose: String) {
    GOOGLE_WORKSPACE(false, "Google Workspace"),
    MICROSOFT_ENTRA_ID(true, "Microsoft Entra ID"),
    OKTA(true, "Okta"),
    KEYCLOAK(true, "Keycloak"),
    GENERIC_OIDC(true, "Generic OIDC");

    override fun toString(): String = verbose

    companion object {
        fun defaultIssuer(preset: SsoPreset): String = when (preset) {
            GOOGLE_WORKSPACE -> "https://accounts.google.com"
            else -> ""
        }

        fun defaultClaimName(preset: SsoPreset): String = when (preset) {
            MICROSOFT_ENTRA_ID -> "roles"
            else -> "groups"
        }
    }
}
