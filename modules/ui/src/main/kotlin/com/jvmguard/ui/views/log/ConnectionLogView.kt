package com.jvmguard.ui.views.log

import com.jvmguard.data.user.Roles
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.connector.api.log.LogFileType
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.PROFILER)
@Route(value = "log/connection", layout = MainLayout::class)
@PageTitle("jvmguard: Connection Log")
class ConnectionLogView : AbstractLogView(LogFileType.CONNECTION, ID) {

    companion object {
        const val ID = "log-view-connection"
    }
}
