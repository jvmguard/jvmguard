package com.jvmguard.rest

import com.jvmguard.rest.provider.AbstractExportHttpMessageConverter
import com.jvmguard.rest.provider.EntityListHttpMessageConverter
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
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
}
