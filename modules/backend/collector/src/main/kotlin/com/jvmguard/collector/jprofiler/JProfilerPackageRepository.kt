package com.jvmguard.collector.jprofiler

import com.install4j.api.update.ApplicationDisplayMode
import com.install4j.api.update.UpdateChecker
import com.jvmguard.common.JvmGuardDirectories
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class JProfilerPackage(val fileName: String, val version: String, val fileSize: Long, val downloadUrl: String)

// JProfiler agent package metadata resolved for a specific target platform
data class PackageRef(val token: String, val pkg: JProfilerPackage) {
    val version: String get() = pkg.version

    // The artifact cache key used on the agent side
    val artifactKey: String get() = "${pkg.version}/$token"
}

class JProfilerUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Component
class JProfilerPackageRepository(private val directories: JvmGuardDirectories) {

    private val updatesUrl: String = System.getProperty("jvmguard.jprofilerUpdatesUrl")
        ?: "$DOWNLOAD_BASE_URL/updates${JProfilerPlatform.MAJOR_VERSION}.xml"

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    @Volatile
    private var cachedPackages: List<JProfilerPackage>? = null

    @Volatile
    private var cachedAtNanos: Long = 0

    fun resolveRef(osName: String, osArch: String): PackageRef {
        val token = JProfilerPlatform.downloadToken(osName, osArch)
            ?: throw JProfilerUnavailableException("JProfiler recording is not supported on $osName / $osArch")
        val prefix = "jprofiler_agent_${token}_"
        val pkg = try {
            descriptor().firstOrNull { it.fileName.startsWith(prefix) }
        } catch (e: Exception) {
            throw JProfilerUnavailableException("Could not read JProfiler update descriptor $updatesUrl: ${e.message}", e)
        } ?: throw JProfilerUnavailableException("No JProfiler agent package for platform '$token' in $updatesUrl")
        return PackageRef(token, pkg)
    }

    fun download(ref: PackageRef): File = ensureDownloaded(ref.pkg)

    private fun descriptor(): List<JProfilerPackage> {
        val cached = cachedPackages
        if (cached != null && System.nanoTime() - cachedAtNanos < DESCRIPTOR_TTL.toNanos()) {
            return cached
        }
        val descriptor = UpdateChecker.getUpdateDescriptor(updatesUrl, ApplicationDisplayMode.UNATTENDED)
        val packages = descriptor.entries
            .filter { it.fileName.startsWith(AGENT_FILE_PREFIX) }
            .map { JProfilerPackage(it.fileName, it.newVersion, it.fileSize, it.getURL().toString()) }
        cachedPackages = packages
        cachedAtNanos = System.nanoTime()
        return packages
    }

    private fun ensureDownloaded(pkg: JProfilerPackage): File {
        val dir = directories.jprofilerPackagesDirectory
        dir.mkdirs()
        val target = File(dir, pkg.fileName)
        if (target.isFile && (pkg.fileSize <= 0 || target.length() == pkg.fileSize)) {
            return target
        }
        LOGGER.info("downloading JProfiler agent package {}", pkg.downloadUrl)
        val temp = File.createTempFile("jprofiler_agent", ".part", dir)
        try {
            val request = HttpRequest.newBuilder(URI.create(pkg.downloadUrl))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(temp.toPath()))
            if (response.statusCode() != 200) {
                throw JProfilerUnavailableException("HTTP ${response.statusCode()} downloading ${pkg.downloadUrl}")
            }
            if (pkg.fileSize > 0 && temp.length() != pkg.fileSize) {
                throw JProfilerUnavailableException("Size mismatch for ${pkg.fileName}: expected ${pkg.fileSize}, got ${temp.length()}")
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
            }
            return target
        } catch (e: JProfilerUnavailableException) {
            throw e
        } catch (e: Exception) {
            throw JProfilerUnavailableException("Could not download ${pkg.downloadUrl}: ${e.message}", e)
        } finally {
            if (temp.exists()) {
                temp.delete()
            }
        }
    }

    companion object {
        const val DOWNLOAD_BASE_URL = "https://download.ej-technologies.com/jprofiler"
        private const val AGENT_FILE_PREFIX = "jprofiler_agent_"
        private val DESCRIPTOR_TTL: Duration = Duration.ofHours(6)
        private val LOGGER = LoggerFactory.getLogger(JProfilerPackageRepository::class.java)
    }
}
