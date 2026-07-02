package com.jvmguard.connector.api.log

import com.jvmguard.data.user.AccessLevel

enum class LogFileType(
    private val verbose: String,
    val loggerName: String?,
    val minimumAccessLevel: AccessLevel,
) {
    EVENT("Event Log", "event", AccessLevel.VIEWER),
    CONNECTION("Connection Log", "connection", AccessLevel.PROFILER),
    SERVER("Server Log", null, AccessLevel.ADMIN);

    override fun toString(): String = verbose
}
