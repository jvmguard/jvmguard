package com.jvmguard.common.export.base

import com.jvmguard.common.io.WinPrintWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class CsvGenerator(out: OutputStream, private val delimiter: Char, lineFeedOnly: Boolean) : AbstractGenerator() {

    private val pw: PrintWriter =
        WinPrintWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).lineFeedOnly(lineFeedOnly)

    private val fields = ArrayList<Field>()

    private var wroteHeader = false
    private var firstArrayStackSize = OUTSIDE_ARRAY

    override fun writeStartObject(): Generator {
        context.push(Context.OBJECT)
        return this
    }

    override fun writeStartObject(name: String): Generator = writeStartObject()

    override fun writeStartArray(): Generator {
        context.push(Context.ARRAY)
        if (firstArrayStackSize == OUTSIDE_ARRAY) {
            firstArrayStackSize = context.size
        }
        return this
    }

    override fun writeStartArray(name: String): Generator = writeStartArray()

    override fun writeUnquoted(name: String, value: String): Generator {
        if (inExportedObject()) {
            fields.add(Field(name, value))
        }
        return this
    }

    override fun write(name: String, value: String?): Generator =
        if (value == null) {
            writeNull(name)
        } else {
            writeUnquoted(name, quote(value))
        }

    override fun write(name: String, value: BigDecimal?): Generator =
        writeUnquoted(name, quote(value.toString()))

    override fun write(name: String, value: Double): Generator =
        writeUnquoted(name, quote(value.toString()))

    private fun inExportedObject(): Boolean =
        context.size == firstArrayStackSize + 1 && context.peek() == Context.OBJECT

    override fun writeEndArray(): Generator {
        if (context.size == firstArrayStackSize) {
            firstArrayStackSize = OUTSIDE_ARRAY
        }
        context.pop()
        return this
    }

    override fun writeEndObject(): Generator {
        if (inExportedObject() && fields.isNotEmpty()) {
            if (!wroteHeader) {
                for (i in fields.indices) {
                    val field = fields[i]
                    pw.print(quote(field.name))
                    if (i < fields.size - 1) {
                        pw.print(delimiter)
                    }
                }
                pw.println()
                wroteHeader = true
            }
            for (i in fields.indices) {
                val field = fields[i]
                pw.print(field.value)
                if (i < fields.size - 1) {
                    pw.print(delimiter)
                }
            }
            pw.println()
            fields.clear()
        }
        context.pop()
        return this
    }

    override fun write(value: String): Generator = throw UnsupportedOperationException()

    override fun write(value: BigDecimal): Generator = throw UnsupportedOperationException()

    override fun write(value: Int): Generator = throw UnsupportedOperationException()

    override fun write(value: Long): Generator = throw UnsupportedOperationException()

    override fun write(value: Double): Generator = throw UnsupportedOperationException()

    override fun write(value: Boolean): Generator = throw UnsupportedOperationException()

    override fun writeNull(): Generator = throw UnsupportedOperationException()

    override fun close() {
        pw.close()
    }

    override fun flush() {
        pw.flush()
    }

    override fun supportsOptional(): Boolean = false

    private class Field(val name: String, val value: String)

    companion object {
        private const val OUTSIDE_ARRAY = -2

        private fun quote(value: String): String =
            '"' + value.replace("\"", "\"\"") + '"'
    }
}
