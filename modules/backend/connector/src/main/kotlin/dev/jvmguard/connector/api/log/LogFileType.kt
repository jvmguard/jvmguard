package dev.jvmguard.connector.api.log

import dev.jvmguard.data.user.AccessLevel

enum class LogFileType(
    private val verbose: String,
    val loggerName: String?,
    val minimumAccessLevel: AccessLevel,
) {
    EVENT("Event Log", "event", AccessLevel.VIEWER),
    CONNECTION("Connection Log", "connection", AccessLevel.PROFILER),
    AUDIT("Audit Log", "audit", AccessLevel.ADMIN),
    SERVER("Server Log", null, AccessLevel.ADMIN);

    override fun toString(): String = verbose
}
