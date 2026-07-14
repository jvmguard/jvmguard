package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.data.vmdata.SparkLineRange
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.VmIdentifier
import dev.jvmguard.ui.server.Sessions
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.QueryParameters

object TelemetryNavigation {

    fun open(selection: VmIdentifier, telemetryType: TelemetryType, range: SparkLineRange) {
        Sessions.vmSelectionModel().set(selection)
        val params = LinkedHashMap<String, List<String>>()
        params[VmTelemetryView.PARAM_TYPE] = listOf(telemetryType.telemetryIdentifier.mainId)
        params[VmTelemetryView.PARAM_RANGE] = listOf(range.name)
        telemetryType.searchSubIdForTelemetry.takeIf { it.isNotEmpty() }
            ?.let { params[VmTelemetryView.PARAM_SUB] = listOf(it) }
        UI.getCurrent().navigate(VmTelemetryView::class.java, QueryParameters(params))
    }

    fun openOverview(selection: VmIdentifier) {
        Sessions.vmSelectionModel().set(selection)
        val params = mapOf(VmTelemetryView.PARAM_MODE to listOf(VmTelemetryView.MODE_OVERVIEW))
        UI.getCurrent().navigate(VmTelemetryView::class.java, QueryParameters(params))
    }
}
