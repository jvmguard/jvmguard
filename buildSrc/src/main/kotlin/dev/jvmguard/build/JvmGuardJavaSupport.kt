package dev.jvmguard.build

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

abstract class JavacPluginExportsProvider @Inject constructor(
    @get:Input val javaVersion: Property<String>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        val version = javaVersion.get()
        return if (version != JAVA_BASELINE_VERSION.toString() && version != "1.8") {
            // For javac compiler plugins in forked compilation.
            listOf(
                "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED"
            )
        } else {
            emptyList()
        }
    }
}

abstract class ClassFileVersionArgumentProvider @Inject constructor(
    @get:Input @get:Optional val classFileVersion: Property<String>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
        classFileVersion.orNull?.let { listOf("-source", it, "-target", it) } ?: emptyList()
}

fun Project.configureJavaCompile(ext: JvmGuardJavaExtension) {
    val javaVersionProp = ext.javaVersion
    val classFileVersionProp = ext.classFileVersion
    val toolchains = objects.newInstance<InjectedJavaToolchainService>().toolchains

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation,unchecked")
        if (System.getProperty("org.gradle.daemon") != "false") {
            options.isIncremental = true
        }

        options.release.set(javaVersionProp.flatMap { version ->
            if (version == "1.8" || classFileVersionProp.isPresent) {
                provider<Int> { null }
            } else {
                provider { releaseOf(version) }
            }
        })

        javaCompiler.set(
            toolchains.compilerFor {
                languageVersion.set(javaVersionProp.map { JavaLanguageVersion.of(releaseOf(it)) })
            }
        )

        options.compilerArgumentProviders.add(objects.newInstance<ClassFileVersionArgumentProvider>(classFileVersionProp))
        options.forkOptions.jvmArgumentProviders.add(objects.newInstance<JavacPluginExportsProvider>(javaVersionProp))
    }
}
