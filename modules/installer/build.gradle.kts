import dev.jvmguard.build.*
import com.install4j.gradle.Install4jTask

plugins {
    id("kotlin-module")
    id("com.install4j.gradle")
}

configureInstall4j()

sourceSets.create("installerTest")

dependencies {
    "installerTestImplementation"(platform(libs.junit.bom))
    "installerTestImplementation"("org.junit.jupiter:junit-jupiter")
    "installerTestImplementation"(libs.install4j.test)
    "installerTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

val fullVersion = getProductVersion("jvmguard")

val winCertPath = providers.gradleProperty("winCertPath").orNull
val macCertPath = providers.gradleProperty("macCertPath").orNull
val appleIssuerId = providers.gradleProperty("appleIssuerId").orNull
val appleKeyId = providers.gradleProperty("appleKeyId").orNull
val applePrivateApiKey = providers.gradleProperty("applePrivateApiKey").orNull
val digestSigningCommandLine = providers.gradleProperty("digestSigningCommandLine").orNull

tasks {
    fun registerBuildMedia(
        name: String,
        mediaTypes: List<String>?,
        destinationDir: File? = null,
    ): TaskProvider<Install4jTask> =
        register<Install4jTask>(name) {
            dependsOn(":dist")

            val fullVersionForFileName = fullVersion.replace('.', '_')
            val majorVersion = getMajorVersion(fullVersion)

            projectFile = file("jvmguard.install4j")
            release = fullVersion
            macKeystorePassword = ""

            if (mediaTypes != null) {
                this.mediaTypes.set(mediaTypes)
            }
            if (destinationDir != null) {
                destination.set(destinationDir)
            }

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

            if (destinationDir == null) {
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
        }

    val buildMedia = registerBuildMedia("buildMedia", null)
    val buildMediaLinux = registerBuildMedia("buildMediaLinux", listOf("unixInstaller", "unixArchive"))

    val installerTestMediaDir = layout.buildDirectory.dir("installerTestMedia")
    val buildInstallerTestMedia =
        registerBuildMedia("buildInstallerTestMedia", listOf("unixInstaller"), installerTestMediaDir.get().asFile)

    register<Test>("installerTest") {
        dependsOn(buildInstallerTestMedia)

        testClassesDirs = sourceSets["installerTest"].output.classesDirs
        classpath = sourceSets["installerTest"].runtimeClasspath
        useJUnitPlatform()
        jvmArgs("--enable-native-access=ALL-UNNAMED")

        val mediaFileName = "jvmguard_unix_installer_${fullVersion.replace('.', '_')}.sh"
        systemProperty("test.media", installerTestMediaDir.get().asFile.resolve(mediaFileName).absolutePath)

        doFirst {
            environment("INSTALL4J_JAVA_HOME", javaLauncher.get().metadata.installationPath.toString())
        }
    }

    val media = register("media") {
        dependsOn(buildMedia)
    }

    register("mediaLinux") {
        dependsOn(buildMediaLinux)
    }

    register("release") {
        description = "Publishes to Maven Central and marks the draft GitHub release as published"
        dependsOn(
                ":agent:api:publishAndReleaseToMavenCentralGithub",
                "publishGithubRelease"
        )
    }

    register("overwriteRelease") {
        mustRunAfter(media)
        val tag = "v$fullVersion"
        doLastWith(tag, execOperations) { releaseTag, execOps ->
            writeGitTag(execOps, releaseTag, force = true)
        }
    }

    val draftGithubRelease = register("draftGithubRelease") {
        dependsOn(":extractReleaseNotes")
        mustRunAfter(media, "overwriteRelease")
        val version = fullVersion
        val notesFile = mediaDir.parentFile.resolve("build/gradle/release-notes.md")
        val media = mediaDir
        // Tag first, so the draft release is attached to an existing tag
        doLastWith(getReleaseTag("jvmguard"), execOperations) { releaseTag, execOps ->
            writeGitTag(execOps, releaseTag)
        }
        doLastWith(execOperations, version, notesFile, media) { execOps, ver, notes, mediaDirectory ->
            val tag = "v$ver"
            publishGithubRelease(execOps, tag, ver, notes, mediaDirectory, draft = true)
        }
    }

    register("publishGithubRelease") {
        mustRunAfter(draftGithubRelease)
        val version = fullVersion
        doLastWith(execOperations, version) { execOps, ver ->
            publishDraftGithubRelease(execOps, "v$ver")
            println("Triggering docs/Pages rebuild")
            execOps.exec {
                commandLine("gh", "workflow", "run", "docs.yml")
                isIgnoreExitValue = true
            }
        }
    }
}
