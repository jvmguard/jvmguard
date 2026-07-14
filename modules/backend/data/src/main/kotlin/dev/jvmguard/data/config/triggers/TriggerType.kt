package dev.jvmguard.data.config.triggers

enum class TriggerType(
    private val verbose: String,
    private val triggerSettingsClass: Class<out Trigger>,
) {
    CONNECTION("Connection count trigger", ConnectionTrigger::class.java),
    POLICY("Policy trigger", PolicyTrigger::class.java),
    THRESHOLD("Threshold violation trigger", ThresholdTrigger::class.java);

    fun createTrigger(): Trigger = triggerSettingsClass.getDeclaredConstructor().newInstance()

    override fun toString(): String = verbose
}
