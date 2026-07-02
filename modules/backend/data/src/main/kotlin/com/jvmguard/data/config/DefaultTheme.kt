package com.jvmguard.data.config

enum class DefaultTheme(private val verbose: String) {
    LIGHT("Light"),
    DARK("Dark");

    override fun toString(): String = verbose
}
