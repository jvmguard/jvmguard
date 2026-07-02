package com.jvmguard.data.user

enum class AccessLevel(private val verbose: String, val isAllGroups: Boolean) {
    VIEWER("Viewer", true),
    PROFILER("Profiler", false),
    ADMIN("Admin", true);

    override fun toString(): String = verbose

    fun isAtLeast(accessLevel: AccessLevel): Boolean = ordinal >= accessLevel.ordinal

    companion object {
        val DEFAULT: AccessLevel = PROFILER
    }
}
