import dev.jvmguard.build.*
import com.install4j.gradle.Install4jTask

plugins {
    id("java-module")
    id("com.install4j.gradle")
}

configureInstall4j()

dependencies {
    compileOnly(libs.install4j.runtime)
}

val fullVersion = getProductVersion("jvmguard")

val winCertPath = providers.gradleProperty("winCertPath").orNull
val macCertPath = providers.gradleProperty("macCertPath").orNull
val appleIssuerId = providers.gradleProperty("appleIssuerId").orNull
val appleKeyId = providers.gradleProperty("appleKeyId").orNull
val applePrivateApiKey = providers.gradleProperty("applePrivateApiKey").orNull
val digestSigningCommandLine = providers.gradleProperty("digestSigningCommandLine").orNull

tasks {
    val jar = named<Jar>("jar") {
        include("dev/jvmguard/installer/**")
    }

    val buildMedia = register<Install4jTask>("buildMedia") {
        dependsOn(jar, ":dist")

        val fullVersionForFileName = fullVersion.replace('.', '_')
        val majorVersion = getMajorVersion(fullVersion)

        projectFile = file("jvmguard.install4j")
        release = fullVersion
        macKeystorePassword = ""

        if (winCertPath == null && macCertPath == null) {
            disableSigning = true
        }
        if (appleIssuerId == null || appleKeyId == null || applePrivateApiKey == null) {
            disableNotarization = true
        }

        variables = mapOf(
            "majorVersion" to majorVersion,
            "build" to getBuildNumber("jvmguard"),
            "winCertPath" to (winCertPath ?: ""),
            "macCertPath" to (macCertPath ?: ""),
            "digestSigningCommandLine" to (digestSigningCommandLine ?: ""),
            "appleIssuerId" to (appleIssuerId ?: ""),
            "appleKeyId" to (appleKeyId ?: ""),
            "applePrivateApiKey" to (applePrivateApiKey ?: ""),
        )

        vmParameters.add("--enable-native-access=ALL-UNNAMED")

        doFirstWith(fileSystemOperations, mediaDir) { fsOps, mediaDir ->
            fsOps.delete { delete(mediaDir) }
        }

        val checksumFile = File("$mediaDir/sha256sums")
        val checksumTargetFile = file("$mediaDir/sha256sums_$fullVersionForFileName.txt")
        doLastWith(checksumFile, checksumTargetFile) {
                checksumFile, checksumTargetFile ->
            checksumFile.renameTo(checksumTargetFile)
        }
    }

    val media = register("media") {
        dependsOn(buildMedia)
    }

    register("release") {
        mustRunAfter(media)
        dependsOn(
                ":agent:api:publishAndReleaseToMavenCentral"
        )
        doLastWith(getReleaseTag("jvmguard"), execOperations) { releaseTag, execOps ->
            writeGitTag(execOps, releaseTag)
        }
    }

    register("overwriteRelease") {
        mustRunAfter(media)
        val tag = "v$fullVersion"
        doLastWith(tag, execOperations) { releaseTag, execOps ->
            writeGitTag(execOps, releaseTag, force = true)
        }
    }

    register("publishGithubRelease") {
        dependsOn("extractReleaseNotes")
        mustRunAfter("release", "overwriteRelease")
        val version = fullVersion
        val notesFile = mediaDir.parentFile.resolve("build/gradle/release-notes.md")
        val media = mediaDir
        doLastWith(execOperations, version, notesFile, media) { execOps, ver, notes, mediaDirectory ->
            val tag = "v$ver"
            publishGithubRelease(execOps, tag, ver, notes, mediaDirectory)
            println("Triggering docs/Pages rebuild")
            execOps.exec {
                commandLine("gh", "workflow", "run", "docs.yml")
                isIgnoreExitValue = true
            }
        }
    }
}
