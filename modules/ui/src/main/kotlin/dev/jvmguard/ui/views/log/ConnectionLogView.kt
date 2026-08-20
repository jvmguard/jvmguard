package dev.jvmguard.ui.views.log

import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.connector.api.log.LogFileType
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.PROFILER)
@Route(value = "log/connection", layout = MainLayout::class)
class ConnectionLogView : AbstractLogView(LogFileType.CONNECTION, ID) {

    override val emptyStateHint: String
        get() = t("log.connection.empty")


    companion object {
        const val ID = "log-view-connection"
    }
}
