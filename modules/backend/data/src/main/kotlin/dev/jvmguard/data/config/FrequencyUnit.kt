package dev.jvmguard.data.config

enum class FrequencyUnit(
    val description: String,
    val label: String,
    val multiplier: Int,
) {
    PER_SECOND("per second", " / s", 1),
    PER_MINUTE("per minute", " / m", 60),
    PER_HOUR("per hour", " / h", 60 * 60);

    override fun toString(): String = description
}
