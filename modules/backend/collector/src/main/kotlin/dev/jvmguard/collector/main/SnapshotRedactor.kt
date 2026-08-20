package dev.jvmguard.collector.main

import dev.jvmguard.data.file.SnapshotFileType
import jdk.jfr.consumer.RecordingFile
import me.bechberger.hprof.HprofRedact
import me.bechberger.hprof.transformer.ZeroPrimitiveTransformer
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object SnapshotRedactor {

    private val LOGGER = LoggerFactory.getLogger(SnapshotRedactor::class.java)

    fun supports(type: SnapshotFileType): Boolean =
        type == SnapshotFileType.HPZ || type == SnapshotFileType.JFR

    fun redact(file: File, type: SnapshotFileType): Long {
        val start = System.nanoTime()
        val length = when (type) {
            SnapshotFileType.HPZ -> redactHprofGzip(file)
            SnapshotFileType.JFR -> redactJfr(file)
            else -> throw IllegalArgumentException("redaction is not supported for $type")
        }
        LOGGER.info(
            "redacted {} {} in {} ms", type, file.name, (System.nanoTime() - start) / 1_000_000
        )
        return length
    }

    private fun redactHprofGzip(file: File): Long {
        val directory = file.absoluteFile.parentFile
        val uncompressed = File.createTempFile("jvmguard-redact", ".hprof", directory)
        val redacted = File.createTempFile("jvmguard-redact", ".hpz", directory)
        try {
            GZIPInputStream(BufferedInputStream(file.inputStream())).use { input ->
                uncompressed.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
            val counting = CountingOutputStream(OutputStream.nullOutputStream())
            GZIPOutputStream(BufferedOutputStream(redacted.outputStream())).use { gzip ->
                val tee = TeeOutputStream(gzip, counting)
                HprofRedact(ZeroPrimitiveTransformer()).process(uncompressed.toPath(), tee)
            }
            Files.move(
                redacted.toPath(), file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            return counting.count
        } catch (e: Exception) {
            redacted.delete()
            throw IOException("could not redact HPROF snapshot ${file.name}", e)
        } finally {
            uncompressed.delete()
        }
    }

    private fun redactJfr(file: File): Long {
        val redacted = File.createTempFile("jvmguard-redact", ".jfr", file.absoluteFile.parentFile)
        try {
            val eventsToDrop = jfrEventsToDrop(file)
            RecordingFile(file.toPath()).use { recordingFile ->
                recordingFile.write(redacted.toPath()) { event ->
                    event.eventType.name !in eventsToDrop
                }
            }
            Files.move(
                redacted.toPath(), file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            return file.length()
        } catch (e: Exception) {
            redacted.delete()
            throw IOException("could not redact JFR snapshot ${file.name}", e)
        }
    }

    /**
     * Determines which event types to drop from the recording in [file].
     *
     * On JDK 27+ (JEP 536), system properties, environment variables, and JVM command-line arguments
     * are redacted in-process with REDACTED placeholders, so the events themselves can be kept.
     * THose events are only dropped if the recording comes from an older JDK, or if in-process redaction was explicitly
     * disabled with "redact-key=none" / "redact-argument=none"
     */
    private fun jfrEventsToDrop(file: File): Set<String> {
        var javaMajor: Int? = null
        var inProcessRedactionDisabled = false
        try {
            RecordingFile(file.toPath()).use { recordingFile ->
                while (recordingFile.hasMoreEvents()) {
                    val event = recordingFile.readEvent()
                    when (event.eventType.name) {
                        "jdk.JVMInformation" ->
                            if (javaMajor == null) {
                                javaMajor = event.getString("jvmVersion")?.let { parseJavaMajorVersion(it) }
                            }

                        "jdk.StringFlag" ->
                            if (event.getString("name") == "FlightRecorderOptions") {
                                val value = event.getString("value").orEmpty()
                                if (value.contains("redact-key=none") || value.contains("redact-argument=none")) {
                                    inProcessRedactionDisabled = true
                                }
                            }
                    }
                }
            }
        } catch (_: Exception) {
            // safe default: drop everything sensitive
        }
        return jfrEventsToDrop(javaMajor, inProcessRedactionDisabled)
    }

    internal fun jfrEventsToDrop(javaMajor: Int?, inProcessRedactionDisabled: Boolean): Set<String> =
        if (javaMajor != null && javaMajor >= JEP_536_JAVA_VERSION && !inProcessRedactionDisabled) {
            ALWAYS_DROPPED_JFR_EVENTS
        } else {
            IN_PROCESS_REDACTED_JFR_EVENTS + ALWAYS_DROPPED_JFR_EVENTS
        }

    internal fun parseJavaMajorVersion(jvmVersion: String): Int? {
        val match = JAVA_VERSION_PATTERN.find(jvmVersion) ?: return null
        val (legacy, major) = match.destructured
        return (major.ifEmpty { legacy }).toIntOrNull()
    }

    private const val JEP_536_JAVA_VERSION = 27

    /**
     * JFR event types whose sensitive *values* JEP 536 (JDK 27+) redacts in-process, so the events
     * can be kept. jdk.EnvironmentVariable is the pre-JDK-19 name of jdk.InitialEnvironmentVariable.
     */
    private val IN_PROCESS_REDACTED_JFR_EVENTS = setOf(
        "jdk.InitialSystemProperty",
        "jdk.InitialEnvironmentVariable",
        "jdk.EnvironmentVariable",
        "jdk.JVMInformation",
    )

    /** JFR event types that JEP 536 does not cover and that are always dropped when redacting. */
    private val ALWAYS_DROPPED_JFR_EVENTS = setOf(
        "jdk.SystemProcess",
        "jdk.ProcessStart",
    )

    // e.g. "OpenJDK 64-Bit Server VM (25.0.3+9-2-Ubuntu) ..." -> 25; "1.8.0_402" -> 8
    private val JAVA_VERSION_PATTERN = Regex("""\b(?:1\.(\d+)\.\d|(\d+)(?:\.\d+)*\+\d)""")

    private class CountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var count: Long = 0
            private set

        override fun write(b: Int) {
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            count += len
        }
    }

    private class TeeOutputStream(
        private val first: OutputStream,
        private val second: OutputStream,
    ) : OutputStream() {
        override fun write(b: Int) {
            first.write(b)
            second.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            first.write(b, off, len)
            second.write(b, off, len)
        }

        override fun flush() {
            first.flush()
            second.flush()
        }

        override fun close() {
            first.close()
            second.close()
        }
    }
}
