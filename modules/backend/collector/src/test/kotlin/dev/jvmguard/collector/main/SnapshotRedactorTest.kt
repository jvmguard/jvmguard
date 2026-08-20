package dev.jvmguard.collector.main

import com.sun.management.HotSpotDiagnosticMXBean
import dev.jvmguard.data.file.SnapshotFileType
import jdk.jfr.Configuration
import jdk.jfr.Event
import jdk.jfr.Name
import jdk.jfr.Recording
import jdk.jfr.consumer.RecordingFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeSet
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class SnapshotRedactorTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `hprof redaction zeroes string contents and keeps a valid heap dump`() {
        val secret = "jvmguard-test-secret-4d71824b"
        // keep strongly reachable so the string survives the live-objects-only dump
        val holder = arrayOf(secret)

        val hpz = tempDir.resolve("heap.hpz")
        val hprof = tempDir.resolve("heap.hprof")
        ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean::class.java).dumpHeap(hprof.toString(), true)
        assertTrue(holder[0] == secret, "the dumped string must stay reachable until after the dump")
        GZIPOutputStream(Files.newOutputStream(hpz)).use { gzip ->
            Files.copy(hprof, gzip)
        }

        assertTrue(contains(gunzip(hpz), secret), "sanity check: the unredacted dump contains the secret")

        val uncompressedLength = SnapshotRedactor.redact(hpz.toFile(), SnapshotFileType.HPZ)

        val stripped = gunzip(hpz)
        assertEquals(stripped.size.toLong(), uncompressedLength, "reported length matches the stripped content")
        assertFalse(contains(stripped, secret), "the secret must be zeroed out")
        assertTrue(String(stripped, 0, 13) == "JAVA PROFILE ", "still a valid HPROF file")
    }

    @Test
    fun `jfr redaction drops sensitive events and keeps the rest`() {
        val jfr = tempDir.resolve("recording.jfr")
        // the "default" configuration records jdk.InitialSystemProperty with VM-start properties
        Recording(Configuration.getConfiguration("default")).use { recording ->
            recording.enable(TestMarkerEvent::class.java)
            recording.start()
            TestMarkerEvent().commit()
            Thread.sleep(1100)
            recording.dump(jfr)
        }

        assertTrue(readEventTypes(jfr).contains("jdk.InitialSystemProperty"), "sanity check: sensitive event recorded")

        SnapshotRedactor.redact(jfr.toFile(), SnapshotFileType.JFR)

        val eventTypes = readEventTypes(jfr)
        if (Runtime.version().feature() >= 27) {
            assertTrue("jdk.InitialSystemProperty" in eventTypes, "kept on JDK 27+: redacted in-process by JEP 536")
        } else {
            assertFalse("jdk.InitialSystemProperty" in eventTypes, "system properties must be scrubbed")
            assertFalse("jdk.InitialEnvironmentVariable" in eventTypes, "environment variables must be scrubbed")
        }
        assertFalse("jdk.SystemProcess" in eventTypes, "process command lines must be scrubbed")
        assertFalse("jdk.ProcessStart" in eventTypes, "process starts must be scrubbed")
        assertTrue("jvmguard.TestMarker" in eventTypes, "unrelated events are kept")
        assertTrue(eventTypes.size > 1, "the recording keeps its remaining events")
    }

    @Test
    fun `java major version is parsed from jvmVersion strings`() {
        assertEquals(25, SnapshotRedactor.parseJavaMajorVersion("OpenJDK 64-Bit Server VM (25.0.3+9-2-24.04.2-Ubuntu) for linux-amd64"))
        assertEquals(27, SnapshotRedactor.parseJavaMajorVersion("OpenJDK 64-Bit Server VM (27+35-LTS) for linux-amd64"))
        assertEquals(21, SnapshotRedactor.parseJavaMajorVersion("OpenJDK 64-Bit Server VM (21.0.7+6-LTS, mixed mode)"))
        assertEquals(8, SnapshotRedactor.parseJavaMajorVersion("OpenJDK 64-Bit Server VM (1.8.0_402-b06, mixed mode)"))
        assertEquals(null, SnapshotRedactor.parseJavaMajorVersion("no version here"))
    }

    @Test
    fun `jep 536 keeps in-process redacted events on jdk 27 plus`() {
        val fullDrop = SnapshotRedactor.jfrEventsToDrop(25, false)
        assertTrue("jdk.InitialSystemProperty" in fullDrop)
        assertTrue("jdk.JVMInformation" in fullDrop)
        assertTrue("jdk.SystemProcess" in fullDrop)

        val jep536Drop = SnapshotRedactor.jfrEventsToDrop(27, false)
        assertFalse("jdk.InitialSystemProperty" in jep536Drop)
        assertFalse("jdk.JVMInformation" in jep536Drop)
        assertTrue("jdk.SystemProcess" in jep536Drop, "not covered by JEP 536, still dropped")
        assertTrue("jdk.ProcessStart" in jep536Drop, "not covered by JEP 536, still dropped")

        val disabledDrop = SnapshotRedactor.jfrEventsToDrop(27, true)
        assertTrue("jdk.InitialSystemProperty" in disabledDrop, "redact-key=none forces the full drop")

        val unknownDrop = SnapshotRedactor.jfrEventsToDrop(null, false)
        assertTrue("jdk.InitialSystemProperty" in unknownDrop, "unknown version falls back to the full drop")
    }

    private fun readEventTypes(jfr: Path): Set<String> {
        val names = TreeSet<String>()
        RecordingFile(jfr).use { recordingFile ->
            while (recordingFile.hasMoreEvents()) {
                names.add(recordingFile.readEvent().eventType.name)
            }
        }
        return names
    }

    private fun gunzip(file: Path): ByteArray {
        val result = ByteArrayOutputStream()
        GZIPInputStream(Files.newInputStream(file)).use { it.copyTo(result) }
        return result.toByteArray()
    }

    private fun contains(haystack: ByteArray, needle: String): Boolean {
        val bytes = needle.toByteArray(Charsets.US_ASCII)
        outer@ for (i in 0..haystack.size - bytes.size) {
            for (j in bytes.indices) {
                if (haystack[i + j] != bytes[j]) continue@outer
            }
            return true
        }
        return false
    }
}

@Name("jvmguard.TestMarker")
class TestMarkerEvent : Event()
