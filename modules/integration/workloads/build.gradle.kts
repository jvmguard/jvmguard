import com.jvmguard.build.*
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode

plugins {
    id("kotlin-module")
}

java {
    disableAutoTargetJvm()
}

tasks.compileKotlin {
    jvmTargetValidationMode.set(JvmTargetValidationMode.IGNORE)
}

// Minimum workloads class file version
classFileVersion = "1.8"

dependencies {
    api(libs.bundles.annotations)
    api(libs.jdom)
    api(project(":agent:api"))
    // Agent classes are provided at runtime by -javaagent.
    compileOnly(project(":agent:bundle"))
}
