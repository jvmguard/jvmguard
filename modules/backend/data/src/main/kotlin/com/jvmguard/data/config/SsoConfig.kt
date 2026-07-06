package com.jvmguard.data.config

import com.jvmguard.data.base.StoredConfig

open class SsoConfig : StoredConfig() {

    var providers: MutableList<SsoProviderConfig> = ArrayList()

    val hasEnabledProviders: Boolean
        get() = providers.any { it.enabled }
}
