package dev.jvmguard.collector.jprofiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JProfilerPackageTest {

    @Test
    fun `token maps supported platforms`() {
        assertEquals("windows-x64", JProfilerPlatform.downloadToken("Windows 11", "amd64"))
        assertEquals("windows-x32", JProfilerPlatform.downloadToken("Windows 10", "x86"))
        assertEquals("macos", JProfilerPlatform.downloadToken("Mac OS X", "x86_64"))
        assertEquals("macos", JProfilerPlatform.downloadToken("Mac OS X", "aarch64"))
        assertEquals("linux-x86", JProfilerPlatform.downloadToken("Linux", "amd64"))
        assertEquals("linux-x86", JProfilerPlatform.downloadToken("Linux", "x86"))
        assertEquals("linux-arm", JProfilerPlatform.downloadToken("Linux", "aarch64"))
        assertEquals("linux-arm", JProfilerPlatform.downloadToken("Linux", "arm"))
    }

    @Test
    fun `token returns null for unsupported platforms`() {
        assertNull(JProfilerPlatform.downloadToken("Linux", "ppc64le"))
        assertNull(JProfilerPlatform.downloadToken("SunOS", "sparcv9"))
    }
}
