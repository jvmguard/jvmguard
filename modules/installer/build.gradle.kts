import com.jvmguard.build.*
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

tasks {
    val jar by existing(Jar::class) {
        include("com/jvmguard/installer/**")
    }

    val buildMedia by registering(Install4jTask::class) {
        dependsOn(jar, ":dist")

        val fullVersionForFileName = fullVersion.replace('.', '_')
        val majorVersion = getMajorVersion(fullVersion)

        projectFile = file("jvmguard.install4j")
        release = fullVersion
        variables = mapOf(
            "majorVersion" to majorVersion,
            "build" to getBuildNumber("jvmguard"),
            "notarizationKey" to getNotarizationKeyFile().canonicalPath
        )

        doFirstWith(fileSystemOperations, mediaDir) { fsOps, mediaDir ->
            fsOps.delete { delete(mediaDir) }
        }

        val updatesXmlFile = file("$mediaDir/updates.xml")
        val updatesXmlTargetFile = file("$mediaDir/updates$majorVersion.xml")
        val checksumFile = File("$mediaDir/sha256sums")
        val checksumTargetFile = file("$mediaDir/sha256sums_$fullVersionForFileName.txt")
        doLastWith(updatesXmlFile, updatesXmlTargetFile, checksumFile, checksumTargetFile) {
                updatesXmlFile, updatesXmlTargetFile, checksumFile, checksumTargetFile ->
            updatesXmlFile.renameTo(updatesXmlTargetFile)
            checksumFile.renameTo(checksumTargetFile)
        }
    }
    provideNotarizationKey(buildMedia)

    val media by registering {
        dependsOn(buildMedia)
    }

    register<Install4jTask>("mediaStandaloneDemo") {
        dependsOn(jar, ":dist")
        projectFile = file("standalone_demo.install4j")
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
        doLastWith(getReleaseTag("jvmguard"), execOperations) { releaseTag, execOps ->
            writeGitTag(execOps, releaseTag)
        }
    }
}
