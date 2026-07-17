package dev.jvmguard.connector.totp

import dev.jvmguard.common.JvmGuardDirectories
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.common.helper.PasswordHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.createTempDirectory

class TotpEncryptionTest {

    private val encryption = TotpEncryption(
        JvmGuardProperties().apply { totpKey = "direct:$KEY_PROPERTY" },
        JvmGuardDirectories.getInstance()
    )

    @Test
    fun encryptedSecretsUseGcmAndRoundTrip() {
        val encrypted = encryption.encryptSecret(SECRET_HEX)
        assertTrue(encrypted.startsWith("v2:"), "new secrets must be encrypted with GCM, got: $encrypted")
        assertEquals(SECRET_HEX, encryption.decryptSecret(encrypted).value)
    }

    @Test
    fun repeatedEncryptionProducesDifferentCiphertexts() {
        assertTrue(encryption.encryptSecret(SECRET_HEX) != encryption.encryptSecret(SECRET_HEX))
    }

    @Test
    fun legacyEcbSecretsStillDecrypt() {
        assertEquals(SECRET_HEX, encryption.decryptSecret(legacyEncrypt(SECRET_HEX)).value)
    }

    @Test
    fun legacyDetection() {
        assertTrue(encryption.isLegacySecret(legacyEncrypt(SECRET_HEX)))
        assertFalse(encryption.isLegacySecret(encryption.encryptSecret(SECRET_HEX)))
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val encrypted = encryption.encryptSecret(SECRET_HEX)
        val decoded = Base64.getDecoder().decode(encrypted.substring(3))
        decoded[decoded.size - 1] = (decoded[decoded.size - 1].toInt() xor 1).toByte()
        val tampered = "v2:" + Base64.getEncoder().encodeToString(decoded)
        assertThrows<Exception> { encryption.decryptSecret(tampered) }
    }

    companion object {
        internal const val KEY_PROPERTY = "jvmguard.test.totpKey"
        private const val SECRET_HEX = "3132333435363738393031323334353637383930"
        internal val KEY_BYTES = ByteArray(16) { (it + 1).toByte() }

        internal fun legacyEncrypt(value: String): String {
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(KEY_BYTES, "AES"))
            return Base64.getEncoder().encodeToString(cipher.doFinal("$value:${PasswordHelper.createHash(value)}".toByteArray()))
        }

        @BeforeAll
        @JvmStatic
        fun setUpDirectories() {
            System.setProperty(KEY_PROPERTY, Base64.getEncoder().encodeToString(KEY_BYTES))
            JvmGuardDirectories.init(createTempDirectory("jvmguard-totp-test").toString(), false, true)
        }
    }
}
