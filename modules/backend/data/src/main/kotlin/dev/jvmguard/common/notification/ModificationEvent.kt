package dev.jvmguard.common.notification

import dev.jvmguard.data.user.User
import org.springframework.context.ApplicationEvent

// Published when server-side state changes so that live connections can refresh
class ModificationEvent(
    source: Any,
    val user: User?,
    val modificationType: ModificationType,
) : ApplicationEvent(source)
