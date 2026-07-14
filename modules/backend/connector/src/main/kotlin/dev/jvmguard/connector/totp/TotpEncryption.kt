package dev.jvmguard.connector.totp

import dev.jvmguard.agent.util.Util
import dev.jvmguard.common.JvmGuardDirectories
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.common.helper.PasswordHelper
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.Key
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Component
class TotpEncryption(
    private val properties: JvmGuardProperties,
    private val directories: JvmGuardDirectories,
) {

    private var secretKey: SecretKey? = null

    private fun algorithm(): String = properties.totpKeyAlgorithm

    @Synchronized
    private fun getSecretKey(): SecretKey {
        secretKey?.let { return it }

        val value: String?
        val configuredKey = properties.totpKey
        val location = (if (configuredKey.isNullOrBlank()) FILE_PREFIX + "totp.key" else configuredKey).trim()
        if (location.startsWith(FILE_PREFIX)) {
            var file = File(location.substring(FILE_PREFIX.length))
            if (!file.isAbsolute) {
                file = File(directories.databaseDirectory, file.path)
            }
            if (file.isFile) {
                value = Files.readString(file.toPath())
            } else {
                val keyGenerator = KeyGenerator.getInstance(algorithm())
                keyGenerator.init(properties.totpKeySize, SecureRandom())
                val generatedKey = keyGenerator.generateKey()
                value = Base64.getEncoder().encodeToString(generatedKey.encoded)
                Files.writeString(file.toPath(), value)
                if (!Util.isWindows()) {
                    Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"))
                }
            }
        } else if (location.startsWith(ENV_PREFIX)) {
            value = System.getenv(location.substring(ENV_PREFIX.length))
        } else if (location.startsWith(DIRECT_PREFIX)) {
            value = System.getProperty(location.substring(DIRECT_PREFIX.length))
        } else {
            throw IllegalArgumentException("Invalid value for property $PROPERTY_TOTP_KEY: $location")
        }

        if (value == null) {
            throw IllegalArgumentException("Value missing for property $PROPERTY_TOTP_KEY")
        }
        return SecretKeySpec(Base64.getDecoder().decode(value), algorithm()).also { secretKey = it }
    }

    fun encryptSecret(secretAsHex: String): String {
        try {
            return encrypt("$secretAsHex:${PasswordHelper.createHash(secretAsHex)}")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun decryptSecret(encryptedValue: String): Secret {
        try {
            val decrypted = decrypt(encryptedValue)
            val index = decrypted.indexOf(":")
            return Secret(decrypted.substring(0, index), decrypted.substring(index + 1))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun encrypt(valueToEnc: String): String {
        val key: Key = getSecretKey()
        val cipher = Cipher.getInstance(algorithm())
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encValue = cipher.doFinal(valueToEnc.toByteArray())
        return Base64.getEncoder().encodeToString(encValue)
    }

    private fun decrypt(encryptedValue: String): String {
        val key: Key = getSecretKey()
        val cipher = Cipher.getInstance(algorithm())
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decValue = Base64.getDecoder().decode(encryptedValue)
        val decValueBytes = cipher.doFinal(decValue)
        return String(decValueBytes)
    }

    data class Secret(val value: String, val hash: String)

    companion object {
        private const val PROPERTY_TOTP_KEY = "jvmguard.totpKey"
        private const val FILE_PREFIX = "file:"
        private const val ENV_PREFIX = "env:"
        private const val DIRECT_PREFIX = "direct:"
    }
}
