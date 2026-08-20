package dev.jvmguard.ui.views.log

import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
import dev.jvmguard.connector.api.log.LogFileType
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "log/audit", layout = MainLayout::class)
class AuditLogView : AbstractLogView(LogFileType.AUDIT, ID) {

    override val emptyStateHint: String
        get() = t("log.audit.empty")

    override val infoText: String
        get() = t("log.audit.info")

    companion object {
        const val ID = "log-view-audit"
    }
}
