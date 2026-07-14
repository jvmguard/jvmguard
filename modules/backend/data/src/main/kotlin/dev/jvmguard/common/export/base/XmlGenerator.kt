package dev.jvmguard.common.export.base

import com.ctc.wstx.stax.WstxOutputFactory
import javanet.staxutils.IndentingXMLStreamWriter
import java.io.IOException
import java.io.OutputStream
import java.math.BigDecimal
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter

class XmlGenerator(private val rootName: String, out: OutputStream, pretty: Boolean) : AbstractGenerator() {

    private var writer: XMLStreamWriter

    private var started = false

    init {
        val xmlOutputFactory: XMLOutputFactory = WstxOutputFactory()
        try {
            var created = xmlOutputFactory.createXMLStreamWriter(out, "UTF-8")
            if (pretty) {
                created = IndentingXMLStreamWriter(created)
            }
            writer = created
        } catch (e: XMLStreamException) {
            throw RuntimeException(e)
        }
    }

    override fun writeStartObject(): Generator {
        writeStart(Context.OBJECT)
        return this
    }

    private fun writeStart(newContext: Context) {
        if ((context.isNotEmpty() && context.peek() != Context.ARRAY) || (context.isEmpty() && started)) {
            throw IllegalArgumentException("cannot be called in this context")
        }
        try {
            if (context.isEmpty()) {
                writer.writeStartDocument("UTF-8", "1.0")
                writer.writeStartElement(rootName)
            } else {
                writer.writeStartElement(ARRAY_ELEMENT_NAME)
            }
            context.push(newContext)
            started = true
        } catch (e: XMLStreamException) {
            throw IOException(e)
        }
    }

    override fun writeStartObject(name: String): Generator {
        writeStart(Context.OBJECT, name)
        return this
    }

    private fun writeStart(newContext: Context, name: String) {
        checkObjectContext()
        try {
            writer.writeStartElement(name)
            context.push(newContext)
        } catch (e: XMLStreamException) {
            throw IOException(e)
        }
    }

    private fun checkObjectContext() {
        if (context.isEmpty() || context.peek() != Context.OBJECT) {
            throw IllegalArgumentException("cannot be called in this context")
        }
    }

    override fun writeStartArray(): Generator {
        writeStart(Context.ARRAY)
        return this
    }

    override fun writeStartArray(name: String): Generator {
        writeStart(Context.ARRAY, name)
        return this
    }

    override fun writeUnquoted(name: String, value: String): Generator {
        checkObjectContext()
        try {
            writer.writeAttribute(getValidAttributeName(name), value)
        } catch (e: XMLStreamException) {
            throw IOException(e)
        }
        return this
    }

    override fun writeEndArray(): Generator = writeEnd()

    override fun writeEndObject(): Generator = writeEnd()

    private fun writeEnd(): Generator {
        if (context.isEmpty()) {
            throw IllegalArgumentException("cannot be called in this context")
        }
        context.pop()
        try {
            writer.writeEndElement()
        } catch (e: XMLStreamException) {
            throw IOException(e)
        }
        return this
    }

    override fun write(value: String): Generator {
        if (context.isEmpty() || context.peek() != Context.ARRAY) {
            throw IllegalArgumentException("cannot be called in this context")
        }
        try {
            writer.writeStartElement(ARRAY_ELEMENT_NAME)
            writer.writeAttribute(VALUE_ATTRIBUTE_NAME, value)
            writer.writeEndElement()
        } catch (e: XMLStreamException) {
            throw IOException(e)
        }
        return this
    }

    override fun write(value: BigDecimal): Generator = write(value.toString())

    override fun write(value: Int): Generator = write(value.toString())

    override fun write(value: Long): Generator = write(value.toString())

    override fun write(value: Double): Generator = write(value.toString())

    override fun write(value: Boolean): Generator = write(value.toString())

    override fun writeNull(): Generator = write(NULL_VALUE)

    override fun close() {
        try {
            writer.close()
        } catch (e: XMLStreamException) {
            throw IOException(e)
        }
    }

    override fun flush() {
        try {
            writer.flush()
        } catch (e: XMLStreamException) {
            throw IOException(e)
        }
    }

    override fun supportsOptional(): Boolean = true

    companion object {
        private const val ARRAY_ELEMENT_NAME = "element"
        private const val VALUE_ATTRIBUTE_NAME = "value"

        private fun getValidAttributeName(name: String): String =
            name.replace('<', '_').replace('>', '_').replace('"', '_').replace('\'', '_').replace(' ', '_')
    }
}
