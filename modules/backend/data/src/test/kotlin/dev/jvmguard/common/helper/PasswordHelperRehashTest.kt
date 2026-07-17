package dev.jvmguard.common.helper

import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.common.JvmGuardProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordHelperRehashTest {

    private val originalProperties = JvmGuardConfig.properties()

    @AfterEach
    fun restoreProperties() {
        JvmGuardConfig.setProperties(originalProperties)
    }

    @Test
    fun hashWithFewerIterationsNeedsRehash() {
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { passwordIterations = 1000 })
        val oldHash = PasswordHelper.createHash("secret")

        JvmGuardConfig.setProperties(JvmGuardProperties().apply { passwordIterations = 100_000 })
        assertTrue(PasswordHelper.needsRehash(oldHash))
        assertTrue(PasswordHelper.validatePassword("secret", oldHash), "old hashes must remain valid")
    }

    @Test
    fun hashWithCurrentIterationsDoesNotNeedRehash() {
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { passwordIterations = 100_000 })
        assertFalse(PasswordHelper.needsRehash(PasswordHelper.createHash("secret")))
    }

    @Test
    fun nonPbkdf2AndEmptyHashesDoNotNeedRehash() {
        assertFalse(PasswordHelper.needsRehash(null))
        assertFalse(PasswordHelper.needsRehash(""))
        assertFalse(PasswordHelper.needsRehash("not-a-hash"))
    }
}
