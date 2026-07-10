package com.jvmguard.collector.jprofiler

object JProfilerPlatform {

    const val MAJOR_VERSION = 16

    fun downloadToken(osName: String, osArch: String): String? {
        val name = osName.lowercase()
        val arch = osArch.lowercase()
        @Suppress("SpellCheckingInspection")
        return when {
            name.startsWith("windows") -> if (arch.contains("64")) "windows-x64" else "windows-x32"
            name.startsWith("mac") || name.contains("os x") || name.contains("darwin") -> "macos"
            name.startsWith("linux") -> when {
                arch == "aarch64" || arch.startsWith("arm") -> "linux-arm"
                arch == "amd64" || arch == "x86_64" || arch == "x86" || arch == "i386" || arch == "i686" -> "linux-x86"
                else -> null
            }
            else -> null
        }
    }

    fun archiveExtension(downloadToken: String): String =
        @Suppress("SpellCheckingInspection")
        when {
            downloadToken.startsWith("windows") -> ".zip"
            downloadToken == "macos" -> ".tgz"
            else -> ".tar.gz"
        }
}
