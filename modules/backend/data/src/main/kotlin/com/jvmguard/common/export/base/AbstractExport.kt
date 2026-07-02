package com.jvmguard.common.export.base

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

abstract class AbstractExport<T : AbstractExport<T>> protected constructor(private val dataName: String) {

    private val exportProperties = LinkedHashMap<String, Any?>()

    protected var gen: Generator? = null
    private var sorted = false
    protected var includeTime = true
    private var timeUnit = TimeUnit.NANOSECONDS

    private var fileType = FileType.JSON
    private var csvSeparator = ','
    private var lineFeedOnly = false
    private var pretty = true

    @Suppress("UNCHECKED_CAST")
    fun sorted(sorted: Boolean): T {
        this.sorted = sorted
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun lineFeedOnly(lineFeedOnly: Boolean): T {
        this.lineFeedOnly = lineFeedOnly
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun includeTime(includeTime: Boolean): T {
        this.includeTime = includeTime
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun timeUnit(timeUnit: TimeUnit): T {
        this.timeUnit = timeUnit
        return this as T
    }

    protected fun isSorted(): Boolean = sorted

    fun addProperty(name: String, value: Any?) {
        if (name == dataName) {
            throw IllegalArgumentException("illegal name $dataName")
        }
        exportProperties[name] = value
    }

    protected abstract fun doExport()

    fun export(out: OutputStream) {
        out.use {
            gen = createGenerator(it)
            try {
                export()
            } finally {
                gen = null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun fileType(fileType: FileType): T {
        if (fileType == FileType.CSV && !isCsvSupported()) {
            throw IllegalArgumentException(fileType.toString())
        }
        this.fileType = fileType
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun csvSeparator(csvSeparator: Char): T {
        this.csvSeparator = csvSeparator
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun pretty(pretty: Boolean): T {
        this.pretty = pretty
        return this as T
    }

    protected fun export() {
        val generator = gen!!
        if (timeUnit != TimeUnit.NANOSECONDS) {
            addProperty("timeUnit", timeUnit.name.lowercase())
        }
        generator.writeStartObject()
        for ((key, value) in exportProperties) {
            writeValue(key, value)
        }

        if (isArray()) {
            generator.writeStartArray(dataName)
        } else {
            generator.writeStartObject(dataName)
        }
        doExport()
        if (isArray()) {
            generator.writeEndArray()
        } else {
            generator.writeEndObject()
        }
        generator.writeEndObject().close()
    }

    protected open fun isArray(): Boolean = false

    open fun isCsvSupported(): Boolean = true

    protected fun convertNanos(time: Long): Long =
        timeUnit.convert(time, TimeUnit.NANOSECONDS)

    protected fun writeValue(name: String, value: Any?) {
        val generator = gen!!
        when (value) {
            null -> generator.writeNull(name)
            is Boolean -> generator.write(name, value)
            is BigDecimal -> generator.write(name, value)
            is Double, is Float -> generator.write(name, (value as Number).toDouble())
            is Number -> generator.write(name, value.toLong())
            is Instant -> generator.write(name, ISO_FORMAT.format(value))
            else -> generator.write(name, value.toString())
        }
    }

    private fun createGenerator(out: OutputStream): Generator {
        val buffered = out as? BufferedOutputStream ?: BufferedOutputStream(out)
        return when (fileType) {
            FileType.XML -> XmlGenerator("data", buffered, pretty)
            FileType.JSON -> JsonGeneratorImpl(buffered, pretty)
            FileType.CSV -> CsvGenerator(buffered, csvSeparator, lineFeedOnly)
        }
    }

    protected fun supportsOptional(): Boolean = gen!!.supportsOptional()

    enum class FileType(private val verbose: String) {
        JSON("JSON"),
        XML("XML"),
        CSV("CSV");

        override fun toString(): String = verbose
    }

    companion object {
        const val PROPNAME_VM: String = "vm"
        const val PROPNAME_VM_TYPE: String = "vmType"
        const val PROPNAME_INTERVAL: String = "interval"
        const val PROPNAME_END_TIME: String = "endTime"

        private val ISO_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
    }
}
