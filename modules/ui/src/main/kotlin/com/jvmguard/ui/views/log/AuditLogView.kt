package com.jvmguard.ui.views.log

import com.jvmguard.data.user.Roles
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.connector.api.log.LogFileType
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "log/audit", layout = MainLayout::class)
@PageTitle("jvmguard: Audit Log")
class AuditLogView : AbstractLogView(LogFileType.AUDIT, ID) {

    override val emptyStateHint: String
        get() = "No audited API access yet."

    override val infoText: String
        get() = "You can revoke API keys under Users & Roles in the general settings with the \"Revoke API key\" row action."

    companion object {
        const val ID = "log-view-audit"
    }
}
