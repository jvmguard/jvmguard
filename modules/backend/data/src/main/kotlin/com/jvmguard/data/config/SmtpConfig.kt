package com.jvmguard.data.config

import com.jvmguard.data.base.StoredConfig

open class SmtpConfig : StoredConfig(), AuthenticationContainer {

    var fromEmail: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var host: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var port: Int = 25
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    private var authenticate: Boolean = false

    override var isAuthenticate: Boolean
        get() = authenticate
        set(value) {
            val old = authenticate
            authenticate = value
            fireChanged(old, value)
        }

    override var userName: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    override var password: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var encryption: Encryption = Encryption.NONE
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

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
