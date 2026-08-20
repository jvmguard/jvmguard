package dev.jvmguard.common

import java.io.IOException
import javax.security.auth.login.CredentialException
import javax.security.auth.login.FailedLoginException

interface LocalizableMessage {
    val messageKey: String
    val messageParams: Array<out Any?> get() = emptyArray()
}

class LocalizedLoginException(
    override val messageKey: String,
    english: String,
    vararg params: Any?,
) : FailedLoginException(english), LocalizableMessage {
    override val messageParams: Array<out Any?> = params
}

class LocalizedCredentialException(
    override val messageKey: String,
    english: String,
    vararg params: Any?,
) : CredentialException(english), LocalizableMessage {
    override val messageParams: Array<out Any?> = params
}

class LocalizedImportException(
    override val messageKey: String,
    english: String,
    cause: Throwable? = null,
    vararg params: Any?,
) : IOException(english, cause), LocalizableMessage {
    override val messageParams: Array<out Any?> = params
}

class LocalizedSecurityException(
    override val messageKey: String,
    english: String,
    vararg params: Any?,
) : SecurityException(english), LocalizableMessage {
    override val messageParams: Array<out Any?> = params
}
