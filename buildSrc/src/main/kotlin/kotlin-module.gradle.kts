import dev.jvmguard.build.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.tasks.UsesKotlinJavaToolchain

plugins {
    id("java-module")
    id("org.jetbrains.kotlin.jvm")
}

val jvmguardJava = the<JvmGuardJavaExtension>()

tasks {
    configure(withType<KotlinJvmCompile>()) {
        val compileTask = this
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_3)
            apiVersion.set(languageVersion.get())
            jvmTarget.set(jvmguardJava.javaVersion.map { version ->
                try {
                    JvmTarget.fromTarget(version)
                } catch (_: IllegalArgumentException) {
                    JvmTarget.JVM_21
                }
            })
            freeCompilerArgs = listOf("-Xskip-metadata-version-check")
            if (!isIdeaSyncActive() && jvmguardJava.javaVersion.get() == JAVA_BASELINE_VERSION.toString()) {
                (compileTask as UsesKotlinJavaToolchain).kotlinJavaToolchain.jdk.use(
                    compileTask.project.getJavaHome(JAVA_BASELINE_VERSION),
                    JAVA_BASELINE_VERSION
                )
            }
        }
    }
}
