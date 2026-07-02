package com.jvmguard.connector.api.log

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

open class LogFileDescriptor(
    val fileName: String,
    private val fileSize: Long,
    private val lastModified: Instant,
    private val current: Boolean,
) {

    @Transient
    private var externalString: String? = null

    val shortDescription: String
        get() = buildVerbose(false)

    override fun toString(): String {
        return externalString ?: buildVerbose(true).also { externalString = it }
    }

    protected fun buildVerbose(longDescription: Boolean): String {
        val buffer = StringBuilder()
        if (!current) {
            buffer.append("[Archived] ")
        }
        buffer.append(File(fileName).name)
        buffer.append(" (")
        buffer.append(fileSize / 1024)
        buffer.append(" kB")
        if (longDescription) {
            buffer.append(", last modified on ")
            buffer.append(DATE_FORMAT.format(lastModified))
        }
        buffer.append(")")
        return buffer.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        return fileName == (other as LogFileDescriptor).fileName
    }

    override fun hashCode(): Int = fileName.hashCode()

    companion object {
        private val DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
}
