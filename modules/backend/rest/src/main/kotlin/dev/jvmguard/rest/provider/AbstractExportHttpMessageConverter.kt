package dev.jvmguard.rest.provider

import dev.jvmguard.common.export.base.AbstractExport
import dev.jvmguard.common.export.base.AbstractExport.FileType
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException

class AbstractExportHttpMessageConverter :
    AbstractHttpMessageConverter<AbstractExport<*>>(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN) {

    override fun supports(clazz: Class<*>): Boolean = AbstractExport::class.java.isAssignableFrom(clazz)

    override fun canRead(clazz: Class<*>, mediaType: MediaType?): Boolean = false

    override fun readInternal(clazz: Class<out AbstractExport<*>>, inputMessage: HttpInputMessage): AbstractExport<*> {
        throw HttpMessageNotReadableException("reading not supported", inputMessage)
    }

    override fun writeInternal(export: AbstractExport<*>, outputMessage: HttpOutputMessage) {
        val subtype = outputMessage.headers.contentType?.subtype
        val fileType = when (subtype) {
            "xml" -> {
                outputMessage.headers.set("Content-Type", "application/xml")
                FileType.XML
            }

            "json" -> {
                outputMessage.headers.set("Content-Type", "application/json;charset=UTF-8")
                FileType.JSON
            }

            else -> {
                outputMessage.headers.set("Content-Type", "text/plain;charset=UTF-8")
                FileType.CSV
            }
        }
        export.fileType(fileType).export(outputMessage.body)
    }
}
