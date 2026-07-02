package com.jvmguard.integration

import com.jvmguard.integration.tests.jvmguard.perf.BasePerfBenchmark
import java.io.File

// Entry point for the standalone agent throughput benchmark. Run via the perfBenchmark Gradle task.
fun main() {
    val javaExecutable = File(System.getProperty("java.home"), "bin/java").absolutePath
    val jdk = JdkUnderTest(Runtime.version().feature(), javaExecutable)
    AgentFixture().execute(BasePerfBenchmark(), jdk)
}
