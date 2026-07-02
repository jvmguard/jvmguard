package com.jvmguard.build

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit

abstract class ServerProcessService : BuildService<BuildServiceParameters.None>, AutoCloseable {

    private var process: Process? = null
    private var port: Int = 8020

    @Synchronized
    fun start(command: List<String>, port: Int = 8020, readyPath: String = "/", logFile: File? = null) {
        if (process != null) {
            throw IllegalStateException("Server process already running")
        }
        this.port = port
        shutdownStaleServer(port)
        val p = ProcessBuilder(command).redirectErrorStream(true).start()
        val out = if (logFile != null) {
            logFile.parentFile?.mkdirs()
            println("appserver: output redirected to ${logFile.absolutePath}")
            PrintStream(FileOutputStream(logFile), true)
        } else {
            System.out
        }
        StreamGobbler(p.inputStream, "appserver", out, closeOut = logFile != null).start()
        process = p

        if (!waitForWebServer("http", "localhost", port, readyPath, 30)) {
            p.destroyForcibly()
            process = null
            throw RuntimeException("Server was not started successfully on port $port")
        }
    }

    @Synchronized
    fun shutdown(): Boolean {
        val p = process ?: return false
        requestGracefulShutdown(port)
        var terminated = p.waitFor(20, TimeUnit.SECONDS)
        if (!terminated) {
            System.err.println("appserver: graceful shutdown on port $port timed out; forcing termination")
            terminated = p.destroyForcibly().waitFor(10, TimeUnit.SECONDS)
            if (!terminated) {
                System.err.println("appserver: process on port $port did not terminate after destroyForcibly")
            }
        }
        process = null
        return terminated
    }

    override fun close() {
        shutdown()
    }

    private fun shutdownStaleServer(port: Int) {
        if (!isPortServing(port)) {
            return
        }
        requestGracefulShutdown(port)
        val endTime = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < endTime && isPortServing(port)) {
            Thread.sleep(300)
        }
    }

    private fun requestGracefulShutdown(port: Int) {
        try {
            val connection = URI("http://localhost:$port/test?command=shutdown").toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 5000
            connection.responseCode
        } catch (_: Exception) {
            // Not a jvmguard server
        }
    }

    private fun isPortServing(port: Int): Boolean =
        try {
            java.net.Socket().use { it.connect(java.net.InetSocketAddress("localhost", port), 500) }
            true
        } catch (_: Exception) {
            false
        }

}

fun waitForWebServer(protocol: String, host: String, port: Int, file: String, timeoutSeconds: Int): Boolean {
    val url = URI("$protocol://$host:$port" + (if (file.startsWith("/")) "" else "/") + file).toURL()
    val endTime = if (timeoutSeconds > 0) System.currentTimeMillis() + timeoutSeconds * 1000L else Long.MAX_VALUE
    while (System.currentTimeMillis() < endTime) {
        try {
            (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2000
                readTimeout = 5000
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return true
                }
            }
        } catch (_: javax.net.ssl.SSLHandshakeException) {
            return true
        } catch (_: Exception) {
        }
        Thread.sleep(500)
    }
    return false
}

private class StreamGobbler(
    private val inputStream: java.io.InputStream,
    private val name: String,
    private val out: PrintStream,
    private val closeOut: Boolean,
) : Thread(name) {
    init {
        isDaemon = true
    }

    override fun run() {
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line -> out.println("$name: $line") }
        }
        if (closeOut) {
            out.close()
        }
    }
}
