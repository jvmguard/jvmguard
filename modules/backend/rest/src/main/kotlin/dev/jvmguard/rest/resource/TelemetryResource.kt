package dev.jvmguard.rest.resource

import dev.jvmguard.common.export.base.AbstractExport
import dev.jvmguard.data.user.RequireViewer
import dev.jvmguard.data.vmdata.TelemetryInterval
import dev.jvmguard.rest.RestHelper
import dev.jvmguard.rest.entity.EntityList
import dev.jvmguard.rest.entity.TelemetryDescriptor
import dev.jvmguard.rest.restInterface.RestInterface
import jakarta.servlet.http.HttpServletRequest
import jakarta.xml.bind.annotation.XmlRootElement
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TelemetryResource(private val restInterface: RestInterface) {

    @GetMapping(
        path = ["/telemetries"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE]
    )
    @RequireViewer
    fun getList(): Telemetries = Telemetries().apply { telemetry = restInterface.getTelemetryDescriptors() }

    @GetMapping(
        path = ["/telemetries/{*name}"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE]
    )
    @RequireViewer
    fun getValues(@PathVariable name: String, request: HttpServletRequest): AbstractExport<*>? {
        val telemetryName = if (name.startsWith("/")) name.substring(1) else name
        val params = RestHelper.getStandardQueryParams(request, TelemetryInterval.TEN_MINUTES)
        return RestHelper.updateParameters(
            restInterface.getTelemetry(params.vmName, params.groupName, telemetryName, params.telemetryInterval!!, params.endTime),
            request
        )
    }

    @XmlRootElement(name = "telemetries")
    class Telemetries : EntityList {
        @JvmField
        var telemetry: List<TelemetryDescriptor>? = null
    }
}
