package com.jvmguard.ui.views.log

import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.connector.api.log.LogFileType
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route(value = "log/event", layout = MainLayout::class)
@PageTitle("jvmguard: Event Log")
class EventLogView : AbstractLogView(LogFileType.EVENT, ID) {

    override val emptyStateHint: String
        get() = "No events yet. Entries appear here when a trigger fires or a diagnostic snapshot is captured."

    companion object {
        const val ID = "log-view-event"
    }
}
