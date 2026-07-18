@file:Suppress("AvoidApplyPluginMethod")

import dev.jvmguard.build.*
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("base-conventions")
    `java-library`
}

if (!project.path.startsWith(":integration")) {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

val jvmguardJava = extensions.create<JvmGuardJavaExtension>("jvmguardJava").apply {
    javaVersion.convention(JAVA_BASELINE_VERSION.toString())
}

configureJavaCompile(jvmguardJava)

if (isIdeaSyncActive()) {
    apply(plugin = "idea")
    afterEvaluate {
        configure<IdeaModel> {
            module {
                jdkName = jvmguardJava.javaVersion.get()
            }
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        // contents of "java.se" (which IDEA does not handle)
        if (jvmguardJava.javaVersion.get() != "1.8" && jvmguardJava.classFileVersion.orNull != "1.8") {
            options.compilerArgs.addAll(
                listOf(
                    "--add-modules",
                    "java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.management"
                )
            )
        }
    }
}
