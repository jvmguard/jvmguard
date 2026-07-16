package dev.jvmguard.build

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.internal.file.AbstractFileCollection
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import org.gradle.process.ExecOperations
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.reflect.KProperty

fun ExtraPropertiesExtension.getOrNull(propertyName: String): Any? =
    if (has(propertyName)) get(propertyName) else null

val AbstractCompile.compileOptions: CompileOptions
    get() = when (this) {
        is JavaCompile -> options
        is GroovyCompile -> options
        else -> throw IllegalStateException(this.javaClass.name)
    }

fun Project.getProductVersion(product: String): String = getProductVersionOrNull(product) ?: throw RuntimeException("Version for $product is not defined")

fun Project.getProductVersionOrNull(product: String): String? {
    val propertyName = "$product.version"
    return when {
        rootProject.hasProperty(propertyName) -> rootProject.property(propertyName).toString()
        rootProject.extra.has(propertyName) -> rootProject.extra[propertyName].toString()
        else -> null
    }
}

fun Project.getReleaseTag(product: String, productVersion: String? = null): String {
    val fullVersion = productVersion ?: getProductVersion(product)
    val releaseRevision = getReleaseRevision(fullVersion)
    return "v$fullVersion${if (releaseRevision != null) "-$releaseRevision" else ""}"
}

@Synchronized
fun writeGitTag(execOps: ExecOperations, tag: String, force: Boolean = false) {
    execOps.exec {
        commandLine(if (force) listOf("git", "tag", "-f", tag) else listOf("git", "tag", tag))
    }
    execOps.exec {
        commandLine(if (force) listOf("git", "push", "origin", "-f", tag) else listOf("git", "push", "origin", tag))
    }
}

private interface InjectedExecOperations {
    @get:Inject
    val execOperations: ExecOperations
}

val Project.execOperations: ExecOperations get() = project.objects.newInstance<InjectedExecOperations>().execOperations

private interface InjectedFileSystemOperations {
    @get:Inject
    val fileSystemOperations: FileSystemOperations
}

val Project.fileSystemOperations: FileSystemOperations get() = project.objects.newInstance<InjectedFileSystemOperations>().fileSystemOperations

private interface InjectedArchiveOperations {
    @get:Inject
    val archiveOperations: ArchiveOperations
}

val Project.archiveOperations: ArchiveOperations get() = project.objects.newInstance<InjectedArchiveOperations>().archiveOperations

// For capturing variables in the configuration phase
fun <T> Task.doFirstWith(value: T, action: Task.(T) -> Unit) {
    doFirst { action(value) }
}

fun <A, B> Task.doFirstWith(a: A, b: B, action: Task.(A, B) -> Unit) {
    doFirst { action(a, b) }
}

fun <A, B, C> Task.doFirstWith(a: A, b: B, c: C, action: Task.(A, B, C) -> Unit) {
    doFirst { action(a, b, c) }
}

fun <A, B, C, D> Task.doFirstWith(a: A, b: B, c: C, d: D, action: Task.(A, B, C, D) -> Unit) {
    doFirst { action(a, b, c, d) }
}

fun <T> Task.doLastWith(value: T, action: Task.(T) -> Unit) {
    doLast { action(value) }
}

fun <A, B> Task.doLastWith(a: A, b: B, action: Task.(A, B) -> Unit) {
    doLast { action(a, b) }
}

fun <A, B, C> Task.doLastWith(a: A, b: B, c: C, action: Task.(A, B, C) -> Unit) {
    doLast { action(a, b, c) }
}

fun <A, B, C, D> Task.doLastWith(a: A, b: B, c: C, d: D, action: Task.(A, B, C, D) -> Unit) {
    doLast { action(a, b, c, d) }
}

fun <A, B, C, D, E> Task.doLastWith(a: A, b: B, c: C, d: D, e: E, action: Task.(A, B, C, D, E) -> Unit) {
    doLast { action(a, b, c, d, e) }
}

fun <A, B, C, D, E, F> Task.doLastWith(a: A, b: B, c: C, d: D, e: E, f: F, action: Task.(A, B, C, D, E, F) -> Unit) {
    doLast { action(a, b, c, d, e, f) }
}

private fun Project.listGitTags(pattern: String): List<String> =
    providers.exec {
        commandLine("git", "tag", "-l", pattern)
    }.standardOutput.asText.get().lines()

@Suppress("UNCHECKED_CAST")
@Synchronized
fun Project.getBuildNumber(product: String): Int {
    val extraName = "buildnumber.cache"
    val cache = when {
        rootProject.extra.has(extraName) -> rootProject.extra[extraName] as MutableMap<String, Int>
        else -> mutableMapOf<String, Int>().also {
            rootProject.extra[extraName] = it
        }
    }

    fun getBuildNumberFromPrefix(tagNamePrefix: String): Int? {
        return listGitTags("$tagNamePrefix*")
            .filter { it.startsWith(tagNamePrefix) }
            .maxOfOrNull { it.substringAfterLast("-").toInt() }
    }

    return cache.getOrPut(product) {
        val majorVersion = getProductVersion(product).substringBefore(".").toInt()
        val previousValue = getBuildNumberFromPrefix("build/$product/$majorVersion") ?: getBuildNumberFromPrefix("build-$product-$majorVersion")
        (previousValue ?: (majorVersion * 1000)) + 1
    }
}

@Synchronized
fun Project.getReleaseRevision(fullVersion: String): Int? {
    val tagNamePrefix = "v$fullVersion"
    val previousValue = listGitTags("$tagNamePrefix*")
        .filter { it.startsWith(tagNamePrefix) }
        .maxOfOrNull { it.substringAfterLast("-", "0").toInt() }
    return if (previousValue != null) {
        previousValue + 1
    } else {
        null
    }
}

fun isIdeaActive() = java.lang.Boolean.getBoolean("idea.active")
fun isIdeaSyncActive() = java.lang.Boolean.getBoolean("idea.sync.active")
fun isIdeaScriptParserActive() = System.getProperty("org.gradle.kotlin.dsl.provider.mode") != null
fun isIdeaRunActive() = isIdeaActive() && !isIdeaSyncActive()

fun isWindows() = System.getProperty("os.name").lowercase(Locale.getDefault()).startsWith("win")
fun isMacos() = System.getProperty("os.name").lowercase(Locale.getDefault()).startsWith("mac")

fun getMajorVersion(version: String): String {
    val index = version.indexOf('.')
    return if (index > -1) {
        val majorVersion = version.substring(0, index)
        if (version.matches(Regex("\\d+\\.9"))) {
            (majorVersion.toInt() + 1).toString()
        } else {
            majorVersion
        }
    } else {
        version
    }
}

fun getPlatformDescriptor(): String = when {
    isWindows() -> "windows"
    isMacos() -> "macos"
    else -> "unix"
}

fun getOutputPaths(projects: Iterable<Project>): Collection<File> {
    return projects
        .filter { it.pluginManager.hasPlugin("java") }
        .map { it.the<JavaPluginExtension>().sourceSets["main"].output }
        .flatMap { it.classesDirs.files + it.resourcesDir }
        .filterNotNull()
}

class NamedTaskExtraPropertyDelegate(private val taskName: String) {
    operator fun getValue(project: Any?, property: KProperty<*>): String? {
        project as Project
        return getTask(project).extra.getOrNull(property.name) as String?
    }

    operator fun setValue(project: Any?, property: KProperty<*>, value: String?) {
        project as Project
        getTask(project).extra[property.name] = value
    }

    private fun getTask(project: Project): Task {
        return project.tasks[taskName]
    }
}

val emptyFileCollection : FileCollection
    get() = object : AbstractFileCollection() {
        override fun getFiles(): Set<File> = emptySet()
        override fun getDisplayName() = "empty file collection"
    }

fun extractChangelogSection(changelog: String, version: String): String {
    val headingPattern = Regex("""(?m)^#{2,3}\s*\[?((?:\d[^\]\n]*|Unreleased))\]?""")
    val matches = headingPattern.findAll(changelog).toList()
    if (matches.isEmpty()) {
        return "Release $version"
    }

    val targetMatch = matches.firstOrNull {
        val headingVersion = it.groupValues[1].trim()
        headingVersion == version || headingVersion.equals("Unreleased", ignoreCase = true)
    } ?: matches.first()

    val startIndex = targetMatch.range.first
    val nextMatch = matches.firstOrNull { it.range.first > targetMatch.range.first }
    val endIndex = nextMatch?.range?.first ?: changelog.length
    return changelog.substring(startIndex, endIndex).trim()
}

fun publishGithubRelease(
    execOps: ExecOperations,
    tag: String,
    version: String,
    notesFile: File,
    mediaDir: File,
    prerelease: Boolean = false
) {
    val mediaFiles = (mediaDir.listFiles()?.toList() ?: emptyList())
        .filter { it.isFile }
        .map { it.absolutePath }

    val exists = execOps.exec {
        commandLine("gh", "release", "view", tag)
        isIgnoreExitValue = true
        standardOutput = java.io.ByteArrayOutputStream()
        errorOutput = java.io.ByteArrayOutputStream()
    }.exitValue == 0

    if (exists) {
        println("Release $tag already exists, uploading assets")
        execOps.exec {
            commandLine("gh", "release", "upload", tag, "--clobber", *mediaFiles.toTypedArray())
        }
        if (notesFile.exists()) {
            execOps.exec {
                commandLine("gh", "release", "edit", tag, "--notes-file", notesFile.absolutePath)
            }
        }
    } else {
        val args = mutableListOf("gh", "release", "create", tag)
        args.addAll(mediaFiles)
        args.addAll(listOf("--title", "jvmguard $version"))
        if (notesFile.exists()) {
            args.addAll(listOf("--notes-file", notesFile.absolutePath))
        }
        if (prerelease) {
            args.add("--prerelease")
        }
        execOps.exec { commandLine(args) }
    }
}
