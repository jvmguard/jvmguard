package dev.jvmguard.ui.views.log

import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.connector.api.log.LogFileType
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route(value = "log/event", layout = MainLayout::class)
class EventLogView : AbstractLogView(LogFileType.EVENT, ID) {

    override val emptyStateHint: String
        get() = t("log.event.empty")

    companion object {
        const val ID = "log-view-event"
    }
}
