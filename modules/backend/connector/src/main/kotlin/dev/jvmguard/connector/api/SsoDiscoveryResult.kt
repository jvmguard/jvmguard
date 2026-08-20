package dev.jvmguard.connector.api

import dev.jvmguard.common.LocalizableMessage

class SsoDiscoveryResult(
    override val messageKey: String,
    vararg params: Any?,
) : LocalizableMessage {
    override val messageParams: Array<out Any?> = params
}
