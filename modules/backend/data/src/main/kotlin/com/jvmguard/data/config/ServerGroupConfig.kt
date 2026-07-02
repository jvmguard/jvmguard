package com.jvmguard.data.config

import com.jvmguard.data.config.thresholds.ThresholdSettings
import com.jvmguard.data.config.triggers.TriggerSettings
import java.io.Serializable

open class ServerGroupConfig : Serializable {
    var thresholdSettings: ThresholdSettings = ThresholdSettings()
    var triggerSettings: TriggerSettings = TriggerSettings()
}
