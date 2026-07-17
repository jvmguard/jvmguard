package dev.jvmguard.common.helper

import com.install4j.runtime.util.Base64
import dev.jvmguard.common.Loggers
import dev.jvmguard.common.JvmGuardConfig
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec

object PasswordHelper {

    private const val NULL_CHAR = '\u0000'

    private val OBFUSCATE_PREFIX = arrayOf(BigDecimal(90), BigDecimal(77), BigDecimal(81), BigDecimal(69))

    private const val ITERATION_INDEX = 0
    private const val SALT_INDEX = 1
    private const val PBKDF2_INDEX = 2

    @Suppress("SpellCheckingInspection")
    private val OBFUSCATE_PASSWORD = "yxcvpaposidufasndfsadfjasdbfoakspojdh".toCharArray()
    private val OBFUSCATE_SALT = byteArrayOf(
        0x56.toByte(), 0x13.toByte(), 0xde.toByte(), 0xd0.toByte(),
        0x09.toByte(), 0x19.toByte(), 0x72.toByte(), 0xa3.toByte(),
    )

    fun createHash(password: String): String =
        try {
            createHash(password.toCharArray())
        } catch (e: GeneralSecurityException) {
            throw RuntimeException(e)
        }

    /**
     * Returns a salted PBKDF2 hash of the password.
     *
     * @param password the password to hash
     * @return a salted PBKDF2 hash of the password
     */
    fun createHash(password: CharArray): String {
        val random = SecureRandom()
        val salt = ByteArray(JvmGuardConfig.properties().passwordSalt)
        random.nextBytes(salt)

        val iterations = JvmGuardConfig.properties().passwordIterations
        val hash = computeHash(password, salt, iterations, JvmGuardConfig.properties().passwordHash)
        if (password.isNotEmpty() && password[0].code == 0) {
            try {
                val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
                val key = keyFactory.generateSecret(PBEKeySpec(OBFUSCATE_PASSWORD))
                val pbeCipher = Cipher.getInstance("PBEWithMD5AndDES")
                pbeCipher.init(Cipher.ENCRYPT_MODE, key, PBEParameterSpec(OBFUSCATE_SALT, 20))
                return getObfuscatePrefix() + Base64.encode(
                    pbeCipher.doFinal(String(password, 1, password.size - 1).toByteArray(StandardCharsets.UTF_8))
                )
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        return iterations.toString() + ":" + Base64.encode(salt) + ":" + Base64.encode(hash)
    }

    /**
     * Validates a password using a hash.
     *
     * @param password    the password to check
     * @param correctHash the hash of the valid password
     * @return true if the password is correct, false if not
     */
    fun validatePassword(password: String, correctHash: String): Boolean =
        try {
            validatePassword(password.toCharArray(), correctHash) == null
        } catch (e: GeneralSecurityException) {
            throw RuntimeException(e)
        }

    /**
     * Returns true if the hash was created with fewer iterations than the current configuration.
     */
    fun needsRehash(hash: String?): Boolean {
        if (hash.isNullOrEmpty()) {
            return false
        }
        val storedIterations = hash.substringBefore(':').toIntOrNull() ?: return false
        return storedIterations < JvmGuardConfig.properties().passwordIterations
    }

    private fun validatePassword(password: CharArray, correctHash: String): String? {
        try {
            val params = correctHash.split(":").toTypedArray()
            val iterations = params[ITERATION_INDEX].toInt()
            val salt = Base64.decode(params[SALT_INDEX])
            val hash = Base64.decode(params[PBKDF2_INDEX])
            val testHash = computeHash(password, salt, iterations, hash.size)
            return if (slowEquals(hash, testHash)) null else ""
        } catch (_: NumberFormatException) {
            try {
                val keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
                val key = keyFactory.generateSecret(PBEKeySpec(OBFUSCATE_PASSWORD))
                val pbeCipher = Cipher.getInstance("PBEWithMD5AndDES")
                pbeCipher.init(Cipher.DECRYPT_MODE, key, PBEParameterSpec(OBFUSCATE_SALT, 20))
                return String(
                    pbeCipher.doFinal(Base64.decode(String(password, 1, password.size - 1))),
                    StandardCharsets.UTF_8
                )
            } catch (e2: RuntimeException) {
                throw e2
            } catch (e2: Exception) {
                throw RuntimeException(e2)
            }
        }
    }

    /**
     * Compares two byte arrays in length-constant time.
     */
    private fun slowEquals(a: ByteArray, b: ByteArray): Boolean {
        var diff = a.size xor b.size
        var i = 0
        while (i < a.size && i < b.size) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
            i++
        }
        return diff == 0
    }

    /**
     * Computes the PBKDF2 hash of a password.
     */
    private fun computeHash(password: CharArray, salt: ByteArray, iterations: Int, bytes: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, bytes * 8)
        val skf = SecretKeyFactory.getInstance(JvmGuardConfig.properties().passwordAlgorithm)
        return skf.generateSecret(spec).encoded
    }

    fun obfuscate(value: String?): String? {
        if (!value.isNullOrEmpty()) {
            try {
                return createHash((NULL_CHAR + value).toCharArray())
            } catch (e: GeneralSecurityException) {
                Loggers.SERVER.warn("Could not obfuscate password", e)
            }
        }
        return value
    }

    fun deobfuscate(value: String?): String? {
        if (value == null || value.length < OBFUSCATE_PREFIX.size) {
            return value
        }
        val prefix = getObfuscatePrefix()
        if (value.startsWith(prefix)) {
            try {
                return validatePassword((NULL_CHAR + value.substring(OBFUSCATE_PREFIX.size)).toCharArray(), NULL_CHAR.toString())
            } catch (e: GeneralSecurityException) {
                Loggers.SERVER.warn("Could not deobfuscate password", e)
            }
        }
        return value
    }

    fun getObfuscatePrefix(): String {
        val chars = CharArray(OBFUSCATE_PREFIX.size)
        for (i in OBFUSCATE_PREFIX.indices) {
            chars[i] = (OBFUSCATE_PREFIX[i].toInt() - 11).toChar()
        }
        return String(chars)
    }
}
