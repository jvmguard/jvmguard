package com.jvmguard.data.config

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class SsoPreset(val supportsGroups: Boolean, val emailAlwaysVerified: Boolean, private val verbose: String) {
    GOOGLE_WORKSPACE(false, true, "Google Workspace"),

    @JsonEnumDefaultValue
    GENERIC_OIDC(true, false, "Generic OIDC");

    override fun toString(): String = verbose

    companion object {
        fun defaultIssuer(preset: SsoPreset): String = when (preset) {
            GOOGLE_WORKSPACE -> "https://accounts.google.com"
            GENERIC_OIDC -> ""
        }
    }
}
