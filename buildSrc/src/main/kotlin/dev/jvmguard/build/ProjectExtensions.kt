@file:Suppress("unused")

package dev.jvmguard.build

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import java.io.File
import java.io.IOException

val Project.buildDirFile: File
    get() = layout.buildDirectory.asFile.get()

val Project.rootBuildDir: File
    get() = rootProject.layout.buildDirectory.dir("gradle").get().asFile

val Project.distDir: File
    get() = rootProject.layout.projectDirectory.dir("dist").asFile

val Project.mediaDir: File
    get() = rootProject.layout.projectDirectory.dir("media").asFile

fun Project.getDependencyProjects(excludes: Iterable<String> = emptyList(), configurationName: String = "api"): Collection<Project> =
    getDependencyProjects(excludes, setOf(configurationName))

fun Project.getDependencyProjects(excludes: Iterable<String> = emptyList(), configurationNames: Set<String>): Collection<Project> {
    if (!pluginManager.hasPlugin("java")) {
        return emptyList()
    }
    val dependencies = mutableSetOf<Project>()
    if (path !in excludes) {
        dependencies.add(this)
    }
    addDependencyProjects(excludes, dependencies, configurationNames)
    return dependencies
}

private fun Project.addDependencyProjects(excludes: Iterable<String>, dependencies: MutableSet<Project>, configurationNames: Set<String>) {
    getDirectDependencyProjects(configurationNames).filter { !excludes.contains(it.project.path) }.forEach { dependencyProject ->
        dependencies.add(dependencyProject.project)
        dependencyProject.project.addDependencyProjects(excludes, dependencies, setOf(dependencyProject.projectDependency.targetConfiguration ?: "api"))
    }
}

class DirectDependencyProject(val project: Project, val projectDependency: ProjectDependency)

fun Project.getDirectDependencyProjects(configurationNames: Set<String>): Collection<DirectDependencyProject> =
    if (plugins.hasPlugin("java")) {
        configurationNames.filter { configurations.names.contains(it) }.flatMap { name ->
            configurations[name].dependencies.withType(ProjectDependency::class.java).map { projectDependency -> DirectDependencyProject(project.project(projectDependency.path), projectDependency) }
        }
    } else {
        emptyList()
    }

fun Project.getDependencyLibraries(configurationName: String = "runtimeClasspath"): FileCollection =
    if (!pluginManager.hasPlugin("java")) {
        emptyFileCollection
    } else {
        configurations.getByName(configurationName).incoming.artifactView {
            componentFilter { it !is ProjectComponentIdentifier }
        }.files
    }

@Suppress("UNCHECKED_CAST")
fun <T> Project.projectProperty(propertyName: String, defaultValue: T): T =
    if (hasProperty(propertyName)) property(propertyName) as T else defaultValue

@Suppress("UNCHECKED_CAST")
fun <T> Project.projectPropertyOrNull(propertyName: String): T? =
    if (hasProperty(propertyName)) property(propertyName) as T else null

fun Project.booleanProperty(propertyName: String, defaultValue: Boolean): Boolean =
    if (hasProperty(propertyName)) property(propertyName).toString().toBoolean() else defaultValue

fun Project.getDependencyOutputPaths(excludes: List<String> = emptyList(), configurationName: String = "api"): Collection<File> =
    getOutputPaths(getDependencyProjects(excludes, configurationName).filter { it.path != path })

fun Project.getCommittedRevisionNumber(): Long {
    val refs = providers.exec {
        commandLine("git", "rev-list", "--remotes=origin")
    }.standardOutput.asText.get().lines().filter { it.isNotBlank() }
    var maxRevision = 0L
    (getDependencyProjects() + project).forEach { p ->
        val lastRefInDir = providers.exec {
            workingDir = p.file(".")
            commandLine("git", "log", "-1", "--pretty=format:%H", "--", ".")
        }.standardOutput.asText.get().lines().first().trim()
        val index = refs.indexOf(lastRefInDir)
        if (index == -1) {
            return 999999L
        }
        val revision = 10000L + (refs.size - index)
        if (revision > maxRevision) {
            maxRevision = revision
        }
    }
    return maxRevision
}

fun Project.getJavaHome(javaVersion: Int): String {
    val githubJdk = System.getenv("JAVA_HOME_${javaVersion}_${if (System.getProperty("os.arch") == "aarch64") "AARCH64" else "X64"}")
    if (githubJdk != null) {
        return githubJdk
    }

    val toolchainService = rootProject.extensions.getByType<JavaToolchainService>()
    val launcher = toolchainService.compilerFor {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }.get()
    return launcher.executablePath.asFile.parentFile.parentFile.absolutePath
}

fun Project.getJavadocExecutable(): String =
    File(getJavaHome(JAVA_BASELINE_VERSION), "bin/javadoc${if (isWindows()) ".exe" else ""}").absolutePath


fun Project.getBuildVersion(): Long {
    val versionParts = getProductVersion("jvmguard").split(".")
    val major = versionParts[0].toLong()
    val minor = versionParts[1].toLong()
    if (minor >= 10) {
        throw RuntimeException("minor version must be < 10")
    }
    val revisionNumber = try {
        getCommittedRevisionNumber()
    } catch (e: IOException) {
        if (isIdeaSyncActive()) 0L else throw e
    }
    return major * 10000000 + minor * 1000000 + revisionNumber
}

fun Project.getBuildVersionProvider() = provider { getBuildVersion() }

fun Project.getJavadocExecutableProvider() = provider { getJavadocExecutable() }
