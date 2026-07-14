package dev.jvmguard.data.config

import dev.jvmguard.data.base.StoredConfig

open class SsoConfig : StoredConfig() {

    var providers: MutableList<SsoProviderConfig> = ArrayList()

}
