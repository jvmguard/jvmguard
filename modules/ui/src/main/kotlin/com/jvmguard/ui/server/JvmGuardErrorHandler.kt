package com.jvmguard.ui.server

import com.jvmguard.ui.components.ErrorDialog
import com.vaadin.flow.component.UI
import com.vaadin.flow.server.ErrorEvent
import com.vaadin.flow.server.ErrorHandler
import org.slf4j.LoggerFactory

/**
 * Handles uncaught UI exceptions
 */
class JvmGuardErrorHandler : ErrorHandler {

    override fun error(event: ErrorEvent) {
        LOGGER.error("Uncaught exception", event.throwable)
        UI.getCurrent()?.access { ErrorDialog(event.throwable, reloadOnClose = true).open() }
    }

    private companion object {
        private val LOGGER = LoggerFactory.getLogger(JvmGuardErrorHandler::class.java)
    }
}
