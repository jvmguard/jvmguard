package com.jvmguard.integration

/** The JDK that a workload child JVM is launched on. */
data class JdkUnderTest(val majorVersion: Int, val javaExecutable: String) {
    override fun toString() = "jdk$majorVersion"
}
