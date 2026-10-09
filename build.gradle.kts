import dev.jvmguard.build.*

plugins {
    java
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    listOf(
        ":agent:api", ":agent:bootstrap", ":agent:bundle", ":agent:core", ":agent:java11", ":agent:mbean", ":agent:tests",
        ":backend:collector", ":backend:connector", ":backend:data", ":backend:mcp", ":backend:rest",
        ":demo",
        ":installer",
        ":server",
        ":ui",
    ).forEach { kover(project(it)) }
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

    register("mediaLinux") {
        group = "distribution"
        description = "Builds the distribution and generates only the Linux installer media"
        dependsOn(dist, ":installer:mediaLinux")
    }

    register("draftRelease") {
        group = "release"
        description = "Builds the media, tags the release and creates a draft GitHub release with the media attached"
        dependsOn(media, ":installer:draftGithubRelease")
    }

    register("release") {
        group = "release"
        description = "Publishes the draft GitHub release, publishes to Maven Central and triggers the website deploy"
        dependsOn(":installer:release")
    }

    register("overwriteRelease") {
        group = "release"
        description = "Builds the media and replaces the tag and the draft GitHub release (skips Maven publish)"
        dependsOn(media, ":installer:overwriteRelease", ":installer:draftGithubRelease")
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
