package dev.jvmguard.ui.views.log

import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.connector.api.log.LogFileType
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "log/server", layout = MainLayout::class)
@PageTitle("jvmguard: Server Log")
class ServerLogView : AbstractLogView(LogFileType.SERVER, ID) {

    companion object {
        const val ID = "log-view-server"
    }
}
