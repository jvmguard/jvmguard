package com.jvmguard.data.config

import com.jvmguard.agent.config.base.CheckedString
import com.jvmguard.common.helper.DeepCopy
import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.base.StoredType

@StoredType("global_config")
open class GlobalConfig : StoredConfig() {

    var use2fa: Boolean = false
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var smtpConfig: SmtpConfig = SmtpConfig()

    var ldapConfig: LdapConfig = LdapConfig()
        private set

    var ssoConfig: SsoConfig = SsoConfig()
        private set

    var infiniteTransactionDays: Boolean = false
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var fixedTransactionDays: Int = 60
        set(value) {
            val used = getUsedRetentionDays(value)
            val old = field
            field = used
            fireChanged(old, used)
        }

    var violationDays: Int = 31
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var snapshotFileDays: Int = 30
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var payloadCap: Int = 50000
        private set

    var transactionCap: Int = 20000
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var windowTitle: CheckedString = CheckedString()
        private set

    var defaultTheme: DefaultTheme = DefaultTheme.LIGHT
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var frequencyUnit: FrequencyUnit = FrequencyUnit.PER_MINUTE
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var checkForUpdates: Boolean = true
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    val transactionDays: Int
        get() = if (infiniteTransactionDays) Int.MAX_VALUE else fixedTransactionDays

    fun toObfuscatedConfig(): GlobalConfig =
        if (authenticationContainers.any { it.password.isNotEmpty() }) {
            DeepCopy.clone(this).apply { obfuscate() }
        } else {
            this
        }

    private fun obfuscate() {
        authenticationContainers.forEach { it.obfuscate() }
    }

    fun deobfuscate() {
        authenticationContainers.forEach { it.deobfuscate() }
    }

    private val authenticationContainers: List<AuthenticationContainer>
        get() = listOf(smtpConfig, ldapConfig) + ssoConfig.providers

    companion object {
        fun getUsedRetentionDays(retentionDays: Int): Int = maxOf(retentionDays, 2)
    }
}
