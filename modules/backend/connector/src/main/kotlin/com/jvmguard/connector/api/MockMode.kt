package com.jvmguard.connector.api

enum class MockMode {
    NONE,
    SYNTHETIC,
    DEMO;

    val isMock: Boolean
        get() = this != NONE

    companion object {
        fun fromParameterValue(value: String?): MockMode = when (value) {
            null -> NONE
            "demo" -> DEMO
            else -> SYNTHETIC
        }
    }
}

