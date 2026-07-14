package dev.jvmguard.rest.resource

import dev.jvmguard.data.user.RequireViewer
import dev.jvmguard.rest.entity.EntityList
import dev.jvmguard.rest.entity.SingleStringEntity
import dev.jvmguard.rest.restInterface.RestInterface
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlRootElement
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class VmResource(private val restInterface: RestInterface) {

    @GetMapping(
        path = ["/vms"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE]
    )
    @RequireViewer
    fun getVms(
        @RequestParam(name = "group", required = false) group: String?,
        @RequestParam(name = "connected", defaultValue = "false") connected: Boolean
    ): Vms = Vms().apply {
        restInterface.getVms(group, connected).mapTo(vm) { name -> Vm().apply { this.name = name } }
    }

    @XmlRootElement(name = "vms")
    class Vms : EntityList {
        @JvmField
        var vm: MutableList<Vm> = ArrayList()
    }

    @XmlRootElement(name = "vm")
    class Vm : SingleStringEntity {
        @JvmField
        @XmlAttribute
        var name: String? = null

        override fun toString(): String = name ?: ""
    }
}
