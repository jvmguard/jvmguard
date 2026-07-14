package dev.jvmguard.rest.provider

import dev.jvmguard.rest.entity.EntityList
import dev.jvmguard.rest.entity.SingleStringEntity
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlRootElement
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import tools.jackson.databind.ObjectMapper
import java.io.*
import java.nio.charset.StandardCharsets

class EntityListHttpMessageConverter :
    AbstractHttpMessageConverter<EntityList>(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN) {

    override fun supports(clazz: Class<*>): Boolean = EntityList::class.java.isAssignableFrom(clazz)

    override fun canRead(clazz: Class<*>, mediaType: MediaType?): Boolean = false

    override fun readInternal(clazz: Class<out EntityList>, inputMessage: HttpInputMessage): EntityList {
        throw HttpMessageNotReadableException("reading not supported", inputMessage)
    }

    override fun writeInternal(entityList: EntityList, outputMessage: HttpOutputMessage) {
        val subtype = outputMessage.headers.contentType?.subtype
        // The Content-Type must be set before getBody() is called, because that commits the headers.
        when (subtype) {
            "xml" -> {
                outputMessage.headers.set("Content-Type", "application/xml")
                writeXml(entityList, outputMessage.body)
            }

            "json" -> {
                outputMessage.headers.set("Content-Type", "application/json;charset=UTF-8")
                OBJECT_MAPPER.writeValue(outputMessage.body, updateStringList(getList(entityList)))
            }

            else -> {
                outputMessage.headers.set("Content-Type", "text/plain;charset=UTF-8")
                val writer = BufferedWriter(OutputStreamWriter(outputMessage.body, StandardCharsets.UTF_8))
                for (element in getList(entityList)) {
                    writer.write(element.toString())
                    writer.write('\n'.code)
                }
                writer.flush()
            }
        }
    }

    companion object {
        private val OBJECT_MAPPER = ObjectMapper()

        private fun writeXml(entityList: EntityList, out: OutputStream) {
            val writer = BufferedWriter(OutputStreamWriter(out, StandardCharsets.UTF_8))
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            val rootName = rootName(entityList.javaClass)
            val list = getList(entityList)
            if (list.isEmpty()) {
                writer.write("<$rootName/>")
            } else {
                writer.write("<$rootName>")
                for (element in list) {
                    writeXmlElement(writer, element!!)
                }
                writer.write("</$rootName>")
            }
            writer.flush()
        }

        private fun writeXmlElement(writer: Writer, element: Any) {
            val elementClass = element.javaClass
            val childName = rootName(elementClass)
            val builder = StringBuilder()
            builder.append('<').append(childName)
            for (field in elementClass.fields) {
                if (field.isAnnotationPresent(XmlAttribute::class.java)) {
                    val value = try {
                        field.get(element)
                    } catch (e: IllegalAccessException) {
                        throw IOException(e)
                    }
                    if (value != null) {
                        builder.append(' ').append(field.name).append("=\"").append(escapeAttribute(value.toString())).append('"')
                    }
                }
            }
            builder.append("/>")
            writer.write(builder.toString())
        }

        private fun rootName(clazz: Class<*>): String {
            val annotation = clazz.getAnnotation(XmlRootElement::class.java)
            return annotation.name
        }

        private fun escapeAttribute(value: String): String {
            val builder = StringBuilder(value.length)
            for (c in value) {
                when (c) {
                    '&' -> builder.append("&amp;")
                    '<' -> builder.append("&lt;")
                    '>' -> builder.append("&gt;")
                    '"' -> builder.append("&quot;")
                    else -> builder.append(c)
                }
            }
            return builder.toString()
        }

        private fun getList(entityList: EntityList): List<*> {
            for (field in entityList.javaClass.fields) {
                if (List::class.java.isAssignableFrom(field.type)) {
                    try {
                        return field.get(entityList) as List<*>? ?: emptyList<Any>()
                    } catch (e: IllegalAccessException) {
                        throw RuntimeException(e)
                    }
                }
            }
            return emptyList<Any>()
        }

        @Suppress("UNCHECKED_CAST")
        private fun updateStringList(list: List<*>): List<*> {
            var ret = list
            for (i in list.indices) {
                val element = list[i]
                if (element is SingleStringEntity) {
                    if (ret === list) {
                        ret = ArrayList(list)
                    }
                    (ret as MutableList<Any?>)[i] = element.toString()
                }
            }
            return ret
        }
    }
}
