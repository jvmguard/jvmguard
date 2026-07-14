package dev.jvmguard.ui.server

import org.springframework.security.core.context.SecurityContextHolder

/**
 * Runs task on a virtual thread with the Spring Security context propagated.
 * Any UI updates from within task must be wrapped in UI.access { }.
 */
fun runInBackground(task: () -> Unit) {
    val securityContext = SecurityContextHolder.getContext()
    Thread.ofVirtual().start {
        SecurityContextHolder.setContext(securityContext)
        try {
            task()
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
