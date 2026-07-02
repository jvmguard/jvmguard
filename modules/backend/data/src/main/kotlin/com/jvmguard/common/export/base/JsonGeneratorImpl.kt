package com.jvmguard.common.export.base

import tools.jackson.core.JsonGenerator
import tools.jackson.core.ObjectWriteContext
import tools.jackson.core.PrettyPrinter
import tools.jackson.core.json.JsonFactory
import tools.jackson.core.util.DefaultPrettyPrinter
import java.io.OutputStream
import java.math.BigDecimal

class JsonGeneratorImpl(out: OutputStream, pretty: Boolean) : Generator {

    private val delegate: JsonGenerator = JsonFactory().createGenerator(writeContext(pretty), out)

    override fun writeStartObject(): Generator {
        delegate.writeStartObject()
        return this
    }

    override fun writeStartObject(name: String): Generator {
        delegate.writeName(name)
        delegate.writeStartObject()
        return this
    }

    override fun writeStartArray(): Generator {
        delegate.writeStartArray()
        return this
    }

    override fun writeStartArray(name: String): Generator {
        delegate.writeName(name)
        delegate.writeStartArray()
        return this
    }

    override fun write(name: String, value: String?): Generator {
        delegate.writeStringProperty(name, value)
        return this
    }

    override fun write(name: String, value: BigDecimal?): Generator {
        delegate.writeNumberProperty(name, value)
        return this
    }

    override fun write(name: String, value: Int): Generator {
        delegate.writeNumberProperty(name, value)
        return this
    }

    override fun write(name: String, value: Long): Generator {
        delegate.writeNumberProperty(name, value)
        return this
    }

    override fun write(name: String, value: Double): Generator {
        delegate.writeNumberProperty(name, value)
        return this
    }

    override fun write(name: String, value: Boolean): Generator {
        delegate.writeBooleanProperty(name, value)
        return this
    }

    override fun writeNull(name: String): Generator {
        delegate.writeNullProperty(name)
        return this
    }

    override fun writeEndArray(): Generator {
        delegate.writeEndArray()
        return this
    }

    override fun writeEndObject(): Generator {
        delegate.writeEndObject()
        return this
    }

    override fun write(value: String): Generator {
        delegate.writeString(value)
        return this
    }

    override fun write(value: BigDecimal): Generator {
        delegate.writeNumber(value)
        return this
    }

    override fun write(value: Int): Generator {
        delegate.writeNumber(value)
        return this
    }

    override fun write(value: Long): Generator {
        delegate.writeNumber(value)
        return this
    }

    override fun write(value: Double): Generator {
        delegate.writeNumber(value)
        return this
    }

    override fun write(value: Boolean): Generator {
        delegate.writeBoolean(value)
        return this
    }

    override fun writeNull(): Generator {
        delegate.writeNull()
        return this
    }

    override fun supportsOptional(): Boolean = true

    override fun close() {
        delegate.close()
    }

    override fun flush() {
        delegate.flush()
    }

    companion object {
        private fun writeContext(pretty: Boolean): ObjectWriteContext {
            if (!pretty) {
                return ObjectWriteContext.empty()
            }
            return object : ObjectWriteContext.Base() {
                override fun hasPrettyPrinter(): Boolean = true

                override fun getPrettyPrinter(): PrettyPrinter = DefaultPrettyPrinter()
            }
        }
    }
}
