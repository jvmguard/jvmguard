package dev.jvmguard.connector.totp

import java.lang.reflect.UndeclaredThrowableException
import java.math.BigInteger
import java.security.GeneralSecurityException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Implementation of TOTP: Time-based One-time Password Algorithm
 *
 * @author thoeger
 */
object TOTP {

    fun validate(key: String, otp: String): Boolean = validate(step, key, otp)

    fun generate(key: String): String = getOTP(step, key)

    private fun validate(step: Long, key: String, otp: String): Boolean =
        getOTP(step, key) == otp || (step > 0 && getOTP(step - 1, key) == otp)

    private val step: Long
        // 30 seconds StepSize (ID TOTP)
        get() = System.currentTimeMillis() / 30000

    private fun getOTP(step: Long, key: String): String {
        require(step >= 0) { "Step must be greater than or equal to zero." }
        val steps = StringBuilder(step.toString(16).uppercase())
        while (steps.length < 16) {
            steps.insert(0, "0")
        }

        val msg = hexStr2Bytes(steps.toString())
        val k = hexStr2Bytes(key)

        val hash = hmacSha1(k, msg)

        val offset = hash[hash.size - 1].toInt() and 0xf
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)
        val otp = binary % 1000000

        val result = StringBuilder(otp.toString())
        while (result.length < 6) {
            result.insert(0, "0")
        }
        return result.toString()
    }

    /**
     * This method converts HEX string to Byte[]
     */
    fun hexStr2Bytes(hex: String): ByteArray {
        // Adding one byte to get the right conversion
        // values starting with "0" can be converted
        val bArray = BigInteger("10$hex", 16).toByteArray()
        val ret = ByteArray(bArray.size - 1)

        // Copy all the REAL bytes, not the "first"
        System.arraycopy(bArray, 1, ret, 0, ret.size)
        return ret
    }

    private fun hmacSha1(keyBytes: ByteArray, text: ByteArray): ByteArray {
        try {
            val hmac = Mac.getInstance("HmacSHA1")
            val macKey = SecretKeySpec(keyBytes, "RAW")
            hmac.init(macKey)
            return hmac.doFinal(text)
        } catch (gse: GeneralSecurityException) {
            throw UndeclaredThrowableException(gse)
        }
    }
}
