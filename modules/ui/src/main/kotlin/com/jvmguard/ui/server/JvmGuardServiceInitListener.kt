package com.jvmguard.ui.server

import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener

class JvmGuardServiceInitListener : VaadinServiceInitListener {

    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addSessionInitListener { it.session.errorHandler = JvmGuardErrorHandler() }
    }
}
