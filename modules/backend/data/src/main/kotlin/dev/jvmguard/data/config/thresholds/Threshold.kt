package dev.jvmguard.data.config.thresholds

import dev.jvmguard.agent.config.base.CheckedString
import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.data.base.StoredConfig
import dev.jvmguard.data.config.triggers.TimeUnit
import dev.jvmguard.data.vmdata.PersistentTelemetryIdentifier

open class Threshold : StoredConfig() {

    @field:ConfigDoc("Whether this threshold rule is active.")
    private var enabled: Boolean = true

    val isEnabled: Boolean
        get() = enabled

    @field:ConfigDoc("Identifies the telemetry series this threshold watches.")
    var telemetryIdentifier: PersistentTelemetryIdentifier? = null
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Optional custom display name for the threshold, provided as an object with a boolean " +
        "'checked' and a string 'value', where the value applies only when checked is true.")
    var customName: CheckedString = CheckedString()
        private set

    @field:ConfigDoc("Whether the threshold is evaluated per single VM or aggregated across the VM group.")
    var target: Target = Target.SINGLE_VMS
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Lower bound value in raw units, interpreted together with lowerBoundUnitLevel.")
    var lowerBound: Long = 0
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Whether the lower-bound check is active.")
    private var lowerBoundEnabled: Boolean = false

    var isLowerBoundEnabled: Boolean
        get() = lowerBoundEnabled
        set(value) { lowerBoundEnabled = changed(lowerBoundEnabled, value) }

    @field:ConfigDoc("Upper bound value in raw units, interpreted together with upperBoundUnitLevel.")
    var upperBound: Long = 0
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Whether the upper-bound check is active.")
    private var upperBoundEnabled: Boolean = false

    var isUpperBoundEnabled: Boolean
        get() = upperBoundEnabled
        set(value) { upperBoundEnabled = changed(upperBoundEnabled, value) }

    @field:ConfigDoc("Magnitude/unit level applied to lowerBound.")
    var lowerBoundUnitLevel: Int = 0
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Magnitude/unit level applied to upperBound.")
    var upperBoundUnitLevel: Int = 0
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("How long a bound must be violated before the threshold fires (with minimumTimeUnit).")
    var minimumTime: Int = 10
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Time unit for minimumTime.")
    var minimumTimeUnit: TimeUnit = TimeUnit.SECONDS
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Cool-down window suppressing duplicate violations (with inhibitDuplicateTimeUnit).")
    var inhibitDuplicateTime: Int = 1
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Time unit for inhibitDuplicateTime.")
    var inhibitDuplicateTimeUnit: TimeUnit = TimeUnit.MINUTES
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("If true, a continuously ongoing violation is not re-reported within the cool-down.")
    private var inhibitDuplicateForContinuousViolation: Boolean = true

    var isInhibitDuplicateForContinuousViolation: Boolean
        get() = inhibitDuplicateForContinuousViolation
        set(value) { inhibitDuplicateForContinuousViolation = changed(inhibitDuplicateForContinuousViolation, value) }

    override fun toString(): String =
        "Threshold{enabled=$enabled, telemetryIdentifier=$telemetryIdentifier, customName=$customName, " +
                "target=$target, lowerBound=$lowerBound, lowerBoundEnabled=$lowerBoundEnabled, " +
                "upperBound=$upperBound, upperBoundEnabled=$upperBoundEnabled, " +
                "lowerBoundUnitLevel=$lowerBoundUnitLevel, upperBoundUnitLevel=$upperBoundUnitLevel, " +
                "minimumTime=$minimumTime, minimumTimeUnit=$minimumTimeUnit, " +
                "inhibitDuplicateTime=$inhibitDuplicateTime, inhibitDuplicateTimeUnit=$inhibitDuplicateTimeUnit, " +
                "inhibitDuplicateForContinuousViolation=$inhibitDuplicateForContinuousViolation}"

    enum class Target(private val verbose: String) {
        @ConfigDoc("Evaluate per individual VM.")
        SINGLE_VMS("Single VMs"),
        @ConfigDoc("Evaluate on the aggregated VM group.")
        GROUP("VM group");

        override fun toString(): String = verbose
    }
}
