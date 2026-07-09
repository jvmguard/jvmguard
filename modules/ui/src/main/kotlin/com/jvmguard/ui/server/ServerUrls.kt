package com.jvmguard.ui.server

import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.VaadinServletRequest

object ServerUrls {

    fun baseUrl(): String {
        val request = (VaadinService.getCurrentRequest() as? VaadinServletRequest)?.httpServletRequest
            ?: return DEFAULT_BASE_URL
        val port = request.serverPort
        val defaultPort = (request.scheme == "http" && port == 80) || (request.scheme == "https" && port == 443)
        val portPart = if (defaultPort) "" else ":$port"
        return "${request.scheme}://${request.serverName}$portPart${request.contextPath}"
    }

    private const val DEFAULT_BASE_URL = "http://localhost:8020"
}
