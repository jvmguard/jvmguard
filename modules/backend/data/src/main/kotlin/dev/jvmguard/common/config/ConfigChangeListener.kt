package dev.jvmguard.common.config

import dev.jvmguard.data.config.GlobalConfig

interface ConfigChangeListener {
    fun globalConfigChanged(oldConfig: GlobalConfig?, newConfig: GlobalConfig)
    fun groupConfigsChanged()
}
