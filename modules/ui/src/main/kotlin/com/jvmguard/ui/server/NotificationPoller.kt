package com.jvmguard.ui.server

import com.vaadin.flow.component.UI
import com.vaadin.flow.shared.Registration

class NotificationPoller private constructor(private val ui: UI, private val session: UserSession) {

    private var registration: Registration? = null

    private fun poll() {
        // Already on the request thread with the Vaadin session locked, so dispatch synchronously.
        if (!session.pollAndDispatch()) {
            stop()
        }
    }

    fun stop() {
        registration?.remove()
        registration = null
        ui.pollInterval = -1
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500

        fun start(ui: UI, session: UserSession): NotificationPoller {
            val poller = NotificationPoller(ui, session)
            poller.registration = ui.addPollListener { poller.poll() }
            ui.pollInterval = POLL_INTERVAL_MS
            return poller
        }
    }
}
