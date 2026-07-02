package com.jvmguard.common.config

import com.jvmguard.data.config.GlobalConfig

interface ConfigChangeListener {
    fun globalConfigChanged(oldConfig: GlobalConfig?, newConfig: GlobalConfig)
    fun groupConfigsChanged()
}
