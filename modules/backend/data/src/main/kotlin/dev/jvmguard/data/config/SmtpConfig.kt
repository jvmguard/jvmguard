package dev.jvmguard.data.config

import dev.jvmguard.data.base.StoredConfig

open class SmtpConfig : StoredConfig(), AuthenticationContainer {

    var fromEmail: String = ""
        set(value) { field = changed(field, value) }

    var host: String = ""
        set(value) { field = changed(field, value) }

    var port: Int = 25
        set(value) { field = changed(field, value) }

    private var authenticate: Boolean = false

    override var isAuthenticate: Boolean
        get() = authenticate
        set(value) { authenticate = changed(authenticate, value) }

    override var userName: String = ""
        set(value) { field = changed(field, value) }

    override var password: String = ""
        set(value) { field = changed(field, value) }

    var encryption: Encryption = Encryption.NONE
        set(value) { field = changed(field, value) }

    val verbose: String
        get() = if (host.isEmpty()) {
            "<span class=\"error-text\">No SMTP server defined</span>"
        } else {
            buildString {
                append("<b>$host:$port</b>")
                if (encryption != Encryption.NONE) {
                    append(" [$encryption]")
                }
                if (authenticate) {
                    append(", authenticate with user name <b>$userName</b>")
                }
            }
        }

    enum class Encryption(private val verbose: String, val javaMailProperty: String) {
        NONE("None", ""),
        SSL("SSL", "mail.smtp.ssl.enable"),
        STARTTLS("StartTLS", "mail.smtp.starttls.enable");

        override fun toString(): String = verbose
    }
}
