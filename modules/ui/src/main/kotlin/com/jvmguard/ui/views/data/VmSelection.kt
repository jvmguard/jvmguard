package com.jvmguard.ui.views.data

import com.jvmguard.data.vmdata.VmIdentifier
import com.jvmguard.ui.server.Sessions
import com.vaadin.flow.router.QueryParameters

/**
 * The selected group/JVM for the data views lives in the URL as a `?vm=<hierarchy-path>` query
 * parameter so it's bookmarkable, per-tab, survives refresh, and is carried across data
 * views.
 */
object VmSelection {

    const val PARAM = "vm"

    fun fromQuery(parameters: QueryParameters): VmIdentifier {
        val path = parameters.parameters[PARAM]?.firstOrNull()?.takeIf { it.isNotEmpty() }
            ?: return VmIdentifier.ROOT_GROUP_IDENTIFIER
        return resolve(path)
    }

    private fun resolve(path: String): VmIdentifier =
        Sessions.current()?.serverConnection?.namedVms
            ?.firstOrNull { it.hierarchyPath == path }
            ?.qualifiedIdentifier
            ?: VmIdentifier.ROOT_GROUP_IDENTIFIER
}
