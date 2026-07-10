package com.jvmguard.rest

import com.jvmguard.common.AuditLog
import com.jvmguard.rest.provider.AbstractExportHttpMessageConverter
import com.jvmguard.rest.provider.EntityListHttpMessageConverter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class RestMvcConfig : WebMvcConfigurer {

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.favorParameter(false).ignoreAcceptHeader(false).defaultContentType(MediaType.APPLICATION_JSON)
    }

    override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
        builder.addCustomConverter(AbstractExportHttpMessageConverter())
        builder.addCustomConverter(EntityListHttpMessageConverter())
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(AuditInterceptor()).addPathPatterns("/api/**")
    }

    // Denials are logged by the RestExceptionHandler, so they are skipped here
    private class AuditInterceptor : HandlerInterceptor {
        override fun afterCompletion(
            request: HttpServletRequest,
            response: HttpServletResponse,
            handler: Any,
            ex: Exception?,
        ) {
            val method = request.method
            if (method == "GET" || method == "HEAD" || method == "OPTIONS") {
                return
            }
            val status = response.status
            if (status == 403) {
                return
            }
            val outcome = if (status in 200..399) AuditLog.Outcome.OK else AuditLog.Outcome.ERROR
            AuditLog.record(
                "rest", SecurityContextHolder.getContext().authentication?.name,
                "$method ${request.requestURI}", outcome, detail = "status=$status", clientIp = request.remoteAddr,
            )
        }
    }
}
