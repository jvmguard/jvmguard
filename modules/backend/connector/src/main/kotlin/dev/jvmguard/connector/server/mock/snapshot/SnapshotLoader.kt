package dev.jvmguard.connector.server.mock.snapshot

import dev.jvmguard.common.Loggers
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object SnapshotLoader {

    private val MAPPER: ObjectMapper = JsonMapper.builder().build()

    @Volatile
    private var cachedPath: String? = null

    @Volatile
    private var cached: DemoSnapshot? = null

    fun configuredPath(): String? = System.getProperty("jvmguard.demoSnapshot")?.takeIf { it.isNotBlank() }

    fun load(): DemoSnapshot? {
        val path = configuredPath() ?: return null
        val current = cached
        if (current != null && path == cachedPath) {
            return current
        }
        return synchronized(this) {
            if (path == cachedPath && cached != null) {
                cached!!
            } else {
                val loaded = read(File(path)) ?: return@synchronized null
                cachedPath = path
                cached = loaded
                loaded
            }
        }
    }

    private fun read(file: File): DemoSnapshot? = try {
        FileInputStream(file).use { read(it) }
    } catch (e: IOException) {
        Loggers.SERVER.warn("Could not read demo snapshot from {}: {}", file, e.message)
        null
    }

    fun read(input: InputStream): DemoSnapshot? =
        GZIPInputStream(input).use { MAPPER.readValue(it, DemoSnapshot::class.java) }

    fun write(snapshot: DemoSnapshot, file: File) {
        file.parentFile?.mkdirs()
        GZIPOutputStream(file.outputStream()).use { gz ->
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(gz, snapshot)
        }
    }
}
