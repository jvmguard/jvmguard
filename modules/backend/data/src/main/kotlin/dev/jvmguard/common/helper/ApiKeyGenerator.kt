package dev.jvmguard.common.helper

import java.security.SecureRandom
import java.util.Base64

object ApiKeyGenerator {

    fun generate(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
