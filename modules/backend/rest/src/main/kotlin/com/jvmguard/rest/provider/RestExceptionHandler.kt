package com.jvmguard.rest.provider

import com.jvmguard.common.Loggers
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.nio.charset.StandardCharsets

class RestException(message: String?, val status: HttpStatus) : RuntimeException(message)

@RestControllerAdvice
class RestExceptionHandler {

    // A @PreAuthorize denial throws during controller invocation, so it reaches this @RestControllerAdvice
    // before Spring Security's ExceptionTranslationFilter. Map it to 403 here, otherwise the Throwable handler
    // below would turn it into a 500.
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(@Suppress("unused") ex: AccessDeniedException): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).build()

    @ExceptionHandler(RestException::class)
    fun handleRestException(ex: RestException): ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8)
        }
        return ResponseEntity(ex.message, headers, ex.status)
    }

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(ex: Throwable, request: HttpServletRequest): ResponseEntity<Void> {
        SERVER_LOGGER.error("error in rest api {}", request.requestURI, ex)
        return ResponseEntity.internalServerError().build()
    }

    companion object {
        private val SERVER_LOGGER = Loggers.SERVER
    }
}
