package com.jvmguard.connector.totp

import org.apache.commons.codec.binary.Base32
import java.security.SecureRandom

@Suppress("ArrayInDataClass")
data class TOTPData(val issuer: String?, val user: String?, val secret: ByteArray) {

    constructor(secret: ByteArray) : this(null, null, secret)

    val secretAsHex: String
        get() {
            val hexChars = CharArray(secret.size * 2)
            for (j in secret.indices) {
                val v = secret[j].toInt() and 0xFF
                hexChars[j * 2] = HEX_ARRAY[v ushr 4]
                hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
            }
            return String(hexChars)
        }

    val secretAsBase32: String get() = Base32().encodeToString(secret)

    val url: String get() = "otpauth://totp/$issuer:$user?secret=$secretAsBase32&issuer=$issuer"

    companion object {
        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
        private val RND = SecureRandom()

        fun create(): TOTPData = TOTPData(createSecret())

        fun create(issuer: String, user: String): TOTPData = TOTPData(issuer, user, createSecret())

        fun createSecret(): ByteArray {
            val secret = ByteArray(20)
            RND.nextBytes(secret)
            return secret
        }
    }
}
