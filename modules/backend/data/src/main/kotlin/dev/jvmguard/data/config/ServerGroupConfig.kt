package dev.jvmguard.data.config

import dev.jvmguard.data.config.guardrails.GuardrailSettings
import dev.jvmguard.data.config.thresholds.ThresholdSettings
import dev.jvmguard.data.config.triggers.TriggerSettings
import java.io.Serializable

open class ServerGroupConfig : Serializable {
    var thresholdSettings: ThresholdSettings = ThresholdSettings()
    var triggerSettings: TriggerSettings = TriggerSettings()
    var guardrailSettings: GuardrailSettings = GuardrailSettings()
}
