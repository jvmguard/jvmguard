package com.jvmguard.data.config.thresholds

import com.jvmguard.agent.config.base.CheckedString
import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.config.triggers.TimeUnit
import com.jvmguard.data.vmdata.PersistentTelemetryIdentifier

open class Threshold : StoredConfig() {

    private var enabled: Boolean = true

    val isEnabled: Boolean
        get() = enabled

    var telemetryIdentifier: PersistentTelemetryIdentifier? = null
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var customName: CheckedString = CheckedString()
        private set

    var target: Target = Target.SINGLE_VMS
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var lowerBound: Long = 0
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    private var lowerBoundEnabled: Boolean = false

    var isLowerBoundEnabled: Boolean
        get() = lowerBoundEnabled
        set(value) {
            val old = lowerBoundEnabled
            lowerBoundEnabled = value
            fireChanged(old, value)
        }

    var upperBound: Long = 0
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    private var upperBoundEnabled: Boolean = false

    var isUpperBoundEnabled: Boolean
        get() = upperBoundEnabled
        set(value) {
            val old = upperBoundEnabled
            upperBoundEnabled = value
            fireChanged(old, value)
        }

    var lowerBoundUnitLevel: Int = 0
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var upperBoundUnitLevel: Int = 0
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var minimumTime: Int = 10
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var minimumTimeUnit: TimeUnit = TimeUnit.SECONDS
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var inhibitDuplicateTime: Int = 1
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var inhibitDuplicateTimeUnit: TimeUnit = TimeUnit.MINUTES
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    private var inhibitDuplicateForContinuousViolation: Boolean = true

    var isInhibitDuplicateForContinuousViolation: Boolean
        get() = inhibitDuplicateForContinuousViolation
        set(value) {
            val old = inhibitDuplicateForContinuousViolation
            inhibitDuplicateForContinuousViolation = value
            fireChanged(old, value)
        }

    override fun toString(): String =
        "Threshold{enabled=$enabled, telemetryIdentifier=$telemetryIdentifier, customName=$customName, " +
                "target=$target, lowerBound=$lowerBound, lowerBoundEnabled=$lowerBoundEnabled, " +
                "upperBound=$upperBound, upperBoundEnabled=$upperBoundEnabled, " +
                "lowerBoundUnitLevel=$lowerBoundUnitLevel, upperBoundUnitLevel=$upperBoundUnitLevel, " +
                "minimumTime=$minimumTime, minimumTimeUnit=$minimumTimeUnit, " +
                "inhibitDuplicateTime=$inhibitDuplicateTime, inhibitDuplicateTimeUnit=$inhibitDuplicateTimeUnit, " +
                "inhibitDuplicateForContinuousViolation=$inhibitDuplicateForContinuousViolation}"

    enum class Target(private val verbose: String) {
        SINGLE_VMS("Single VMs"),
        GROUP("VM group");

        override fun toString(): String = verbose
    }
}
