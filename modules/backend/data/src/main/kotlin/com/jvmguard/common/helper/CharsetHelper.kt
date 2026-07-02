package com.jvmguard.common.helper

import java.nio.charset.StandardCharsets

object CharsetHelper {

    private val ASCII_ENCODER = StandardCharsets.US_ASCII.newEncoder()

    fun isAsciiOnly(str: String): Boolean = ASCII_ENCODER.canEncode(str)
}
