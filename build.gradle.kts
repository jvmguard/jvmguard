import dev.jvmguard.build.*

plugins {
    java
}

val distTemplateDir = file("dist-template")

tasks {
    named<Delete>("clean") {
        doLastWith(fileSystemOperations, rootBuildDir, distDir, mediaDir) { fsOps, rootBuildDir, distDir, mediaDir ->
            fsOps.delete { delete(rootBuildDir, distDir, mediaDir) }
        }
    }

    val distTemplate = register<Copy>("distTemplate") {
        into(distDir)
        from(distTemplateDir)
    }

    val distProperties = register<Copy>("distProperties") {
        into(file("$distDir/resources"))
        from(file("$distTemplateDir/config"))
        include("application.yaml")
        rename("application.yaml", "application.yaml-default")
    }

    val dist = register("dist") {
        group = "distribution"
        description = "Assembles the full jvmguard distribution into dist/"
        dependsOn(
            distTemplate,
            distProperties,
            ":agent:api:dist",
            ":agent:bootstrap:dist",
            ":demo:dist",
            ":server:dist",
            ":ui:dist"
        )
    }

    register("prepareCodescan") {
        group = "verification"
        description = "Builds the distribution and integration test classes for a code scan"
        dependsOn(dist, ":integration:integrationTestClasses")
    }

    register("allTests") {
        group = "verification"
        description = "Runs every test in the project"
        dependsOn(
            // Module unit tests
            ":backend:data:test",
            ":server:test",
            ":ui:test",
            // UI browser tests
            ":ui:e2eTest",
            ":ui:configE2eTest",
            ":ui:dataE2eTest",
            // Agent integration tests
            ":integration:integrationTest",
        )
    }

    val media = register("media") {
        group = "distribution"
        description = "Builds the distribution and generates the installer media"
        dependsOn(dist, ":installer:media")
    }

    register("release") {
        group = "release"
        description = "Builds the media, publishes to Maven Central and creates a GitHub release"
        dependsOn(media, ":installer:release", ":installer:publishGithubRelease")
    }

    register("overwriteRelease") {
        group = "release"
        description = "Builds the media and creates a GitHub release (skips Maven publish)"
        dependsOn(media, ":installer:overwriteRelease", ":installer:publishGithubRelease")
    }

    val fullVersion = getProductVersion("jvmguard")

    register("printVersion") {
        group = "release"
        description = "Prints the current product version"
        doLast {
            println(fullVersion)
        }
    }

    register("printReleaseTag") {
        group = "release"
        description = "Prints the release tag for the current version"
        val tag = getReleaseTag("jvmguard", fullVersion)
        doLast {
            println(tag)
        }
    }

    register("extractReleaseNotes") {
        group = "release"
        description = "Extracts the changelog section for the current version into build/release-notes.md"
        val version = fullVersion
        val changelogFile = rootProject.file("CHANGELOG.md")
        val outputFile = rootBuildDir.resolve("release-notes.md")
        doLast {
            val changelog = changelogFile.readText()
            val versionSection = extractChangelogSection(changelog, version)
            outputFile.parentFile.mkdirs()
            outputFile.writeText(versionSection)
            println("Release notes written to ${outputFile.absolutePath}")
        }
    }
}
