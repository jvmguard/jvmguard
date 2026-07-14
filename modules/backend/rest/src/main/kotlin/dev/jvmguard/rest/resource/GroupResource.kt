package dev.jvmguard.rest.resource

import dev.jvmguard.data.user.RequireViewer
import dev.jvmguard.rest.entity.EntityList
import dev.jvmguard.rest.entity.GroupEntity
import dev.jvmguard.rest.restInterface.RestInterface
import jakarta.xml.bind.annotation.XmlRootElement
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class GroupResource(private val restInterface: RestInterface) {

    @GetMapping(
        path = ["/groups"],
        produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE]
    )
    @RequireViewer
    fun getList(): Groups = Groups().apply { group = restInterface.getGroups() }

    @XmlRootElement(name = "groups")
    class Groups : EntityList {
        @JvmField
        var group: List<GroupEntity>? = null
    }
}
