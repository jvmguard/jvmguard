package dev.jvmguard.rest.resource

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// Makes the dispatcher servlet authoritative for /api/**. Any request that no specific REST
// controller handles produces a 404, rather than falling through to the Vaadin UI with a 200.
@RestController
class RestApiFallbackController {

    @RequestMapping("/api/**")
    fun notFound(): ResponseEntity<Void> = ResponseEntity.notFound().build()
}
