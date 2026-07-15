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
        variables = mapOf(
            "majorVersion" to majorVersion,
            "build" to getBuildNumber("jvmguard"),
        )

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
        doLastWith(getReleaseTag("jvmguard"), execOperations) { releaseTag, execOps ->
            writeGitTag(execOps, releaseTag)
        }
    }
}
