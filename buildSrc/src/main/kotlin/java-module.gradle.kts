import com.jvmguard.build.*
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.plugins.ide.idea.model.IdeaModel
import javax.inject.Inject

plugins {
    id("base-conventions")
    `java-library`
}

tasks {
    configure(withType<JavaCompile>()) {
        project.afterEvaluate {
            applyJavaVersion(usedJavaVersion, classFileVersion)

            @Suppress("AvoidApplyPluginMethod")
            apply(plugin = "idea")
            configure<IdeaModel> {
                module {
                    jdkName = usedJavaVersion
                }
            }

            classFileVersion?.let {
                sourceCompatibility = it
                targetCompatibility = it
            }

            compileOptions.apply {
                compilerArgs.add("-Xlint:deprecation,unchecked")
                encoding = "UTF-8"
                if (System.getProperty("org.gradle.daemon") != "false") {
                    isIncremental = true
                }
            }
        }
    }
}

fun JavaCompile.applyJavaVersion(javaVersion: String, classFileVersion: String? = null) {
    if (isIdeaActive() && !javaVersion.startsWith("1.") && classFileVersion?.startsWith("1.") != true) {
        // contents of "java.se" (which IDEA does not handle)
        compileOptions.compilerArgs.addAll(
            listOf(
                "--add-modules",
                "java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.management"
            )
        )
    }
    when (javaVersion) {
        "11" -> {
            setSourceAndTarget("11")
            configureFork(11, true)
        }

        "17" -> {
            setSourceAndTarget("11")
            configureFork(17, true)
        }

        "21" -> {
            setSourceAndTarget("21")
            configureFork(21, true)
        }

        "25" -> {
            setSourceAndTarget("25")
            if (classFileVersion == null || classFileVersion == "25") {
                compileOptions.compilerArgs.addAll(
                    listOf(
                        "--add-exports", "java.desktop/sun.awt=ALL-UNNAMED",
                        "--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED"
                    )
                )
            }
        }

        "1.8" -> {
            if (!isIdeaScriptParserActive()) {
                val java8Home = project.getJavaHome(8)
                compileOptions.bootstrapClasspath = project.files(
                    "$java8Home/jre/lib/rt.jar",
                    "$java8Home/jre/lib/jce.jar",
                    "$java8Home/jre/lib/jsse.jar",
                    "$java8Home/jre/lib/jfxrt.jar",
                    "$java8Home/jre/lib/ext/jfxrt.jar",
                    "$java8Home/jre/lib/ext/sunpkcs11.jar"
                )
            }
            setSourceAndTarget("1.8")
            configureFork(8, false)
        }

        else -> {
            throw RuntimeException("Java version $javaVersion not supported")
        }
    }

    if (javaVersion == "1.8") {
        compileOptions.compilerArgs.add("-XDignore.symbol.file") // Do not show warnings for internal API in JRE
    }
    if (JavaVersion.current().isJava8Compatible) {
        compileOptions.compilerArgs.add("-Xlint:-options") // Otherwise javac warns about 1.5 source/target values
    }
}

// Not private: Gradle's newInstance() must be able to subclass it, which a script-private type forbids.
interface InjectedJavaToolchainService {
    @get:Inject
    val toolchains: JavaToolchainService
}

private fun JavaCompile.configureFork(javaVersion: Int, addExports: Boolean) {
    if (!isIdeaScriptParserActive()) {
        javaCompiler = project.objects.newInstance<InjectedJavaToolchainService>().toolchains.compilerFor {
            languageVersion = JavaLanguageVersion.of(javaVersion)
        }

        compileOptions.isFork = true
        compileOptions.forkOptions.apply {
            if (addExports) {
                // For javac compiler plugins
                fun getJdkCompilerExportVmParameters(vararg packageNames: String) =
                    packageNames.toList().flatMap { listOf("--add-exports", "jdk.compiler/$it=ALL-UNNAMED") }
                jvmArgs = getJdkCompilerExportVmParameters(
                    "com.sun.tools.javac.api",
                    "com.sun.tools.javac.util",
                    "com.sun.tools.javac.main",
                    "com.sun.tools.javac.code",
                    "com.sun.tools.javac.tree"
                )
            }
        }
    }
}

private fun AbstractCompile.setSourceAndTarget(version: String) {
    sourceCompatibility = version
    targetCompatibility = version
}
