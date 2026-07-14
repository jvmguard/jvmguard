package dev.jvmguard.build

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.concurrent.TimeUnit

/**
 * Manages the jvmguard demo cluster process (JvmGuardDemoServerStarter) for the web25 data E2E test.
 * Unlike [ServerProcessService] it does not wait for an HTTP endpoint. It just
 * launches the starter. On [shutdown] it sends SIGTERM.
 */
abstract class DemoClusterService : BuildService<BuildServiceParameters.None>, AutoCloseable {

    private var process: Process? = null

    @Synchronized
    fun start(command: List<String>, workingDir: File, logFile: File? = null) {
        if (process != null) {
            throw IllegalStateException("Demo cluster already running")
        }
        val builder = ProcessBuilder(command).directory(workingDir).redirectErrorStream(true)
        val p = builder.start()
        val out = if (logFile != null) {
            logFile.parentFile?.mkdirs()
            println("democluster: output redirected to ${logFile.absolutePath}")
            PrintStream(FileOutputStream(logFile), true)
        } else {
            System.out
        }
        DemoStreamGobbler(p.inputStream, out, closeOut = logFile != null).start()
        process = p
    }

    @Synchronized
    fun shutdown(): Boolean {
        val p = process ?: return false
        p.destroy()
        var terminated = p.waitFor(20, TimeUnit.SECONDS)
        if (!terminated) {
            terminated = p.destroyForcibly().waitFor(10, TimeUnit.SECONDS)
        }
        process = null
        return terminated
    }

    override fun close() {
        shutdown()
    }

    private class DemoStreamGobbler(
        private val inputStream: java.io.InputStream,
        private val out: PrintStream,
        private val closeOut: Boolean,
    ) : Thread("democluster") {
        init {
            isDaemon = true
        }

        override fun run() {
            inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> out.println("democluster: $line") }
            }
            if (closeOut) {
                out.close()
            }
        }
    }
}
