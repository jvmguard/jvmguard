package com.jvmguard.data.config.triggers

import com.jvmguard.data.vmdata.ThresholdIdentifier

open class ThresholdTrigger : DataTrigger() {

    var thresholdIdentifier: ThresholdIdentifier? = null
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

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
