package com.jvmguard.common

import org.jetbrains.annotations.Nullable
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jvmguard")
class JvmGuardProperties {

    var dataDirectory: String = "data"
    var httpPort: Int = 8020
    var isUseHttps: Boolean = false
    var isReverseProxy: Boolean = false
    var reverseProxyHost: String = ""
    var contextPath: String = ""
    var isRestApiEnabled: Boolean = true
    var isMcpEnabled: Boolean = true
    var keystoreName: String = ""
    var keystorePassword: String = ""
    var vmPort: Int = 8847
    var isVmUseSsl: Boolean = false
    var isStartH2Console: Boolean = false

    var schedulerPoolSize: Int = 4
    var writerPoolSize: Int = 16

    @get:Nullable
    var vmEnabledProtocols: String? = null

    @get:Nullable
    var vmEnabledCipherSuites: String? = null
    var commPoolSize: Int = 0
    var certPastValidity: Int = 365
    var certValidity: Int = 365 * 100
    var keyAlgorithm: String = "RSA"
    var keySize: Int = 2048

    @Suppress("SpellCheckingInspection")
    var signatureAlgorithm: String = "SHA256withRSA"
    var smtpConnectionTimeout: String = "60000"
    var smtpTimeout: String = "60000"
    var smtpWriteTimeout: String = "60000"
    var isSmtpTrustAllHosts: Boolean = false
    var mailRetrySeconds: Int = 60
    var passwordAlgorithm: String = "PBKDF2WithHmacSHA1"
    var passwordSalt: Int = 48
    var passwordHash: Int = 64
    var passwordIterations: Int = 1000
    var totpKeyAlgorithm: String = "AES"
    var totpKeySize: Int = 128

    @get:Nullable
    var totpKey: String? = null
    var h2ConsoleAllowOthers: String = "false"
    var h2ConsolePort: Int = 8082
    var restFailedAuthWait: Int = 5
    var windowTitle: String = "jvmguard"
    var isNoPlatformMBean: Boolean = false
    var isNoCollection: Boolean = false
    var nameCacheSize: Int = 20000
    var nameIndexLength: Int = 255
    var maxMemGcIds: Int = 1000000
    var gcDaysMaximum: Int = 7
    var gcDaysMinimum: Int = 1
    var gcStartMinutes: Int = 135
    var gcTimeFrame: Int = 120
    var gcWaitSeconds: Int = 60 * 5
    var isGcDebug: Boolean = false
    var telemetryStorage: MutableMap<Int, Int> = HashMap()
}
