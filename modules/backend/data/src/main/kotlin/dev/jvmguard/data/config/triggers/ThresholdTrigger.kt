package dev.jvmguard.data.config.triggers

import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.data.vmdata.ThresholdIdentifier

@ConfigDoc("Fires when a configured threshold is violated.")
open class ThresholdTrigger : DataTrigger() {

    @field:ConfigDoc("Identifies which threshold's violations this trigger counts.")
    var thresholdIdentifier: ThresholdIdentifier? = null
        set(value) { field = changed(field, value) }

    override val triggerType: TriggerType
        get() = TriggerType.THRESHOLD

    override val description: String
        get() = "$triggerType [$thresholdName: $count violations" +
                (if (interval == Interval.NONE) "" else " per $interval") + "]"

    private val thresholdName: String
        get() {
            val customName = thresholdIdentifier!!.customName
            return if (!customName.isNullOrEmpty()) {
                customName
            } else {
                $$"${$$VARIABLE_TYPE_TELEMETRY_ID:$${thresholdIdentifier!!.telemetryIdentifier}}"
            }
        }

    companion object {
        const val VARIABLE_TYPE_TELEMETRY_ID: String = "ti"
    }
}
