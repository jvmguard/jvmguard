import com.jvmguard.build.*

plugins {
    java
}

val distTemplateDir = file("dist-template")

tasks {
    val clean = named<Delete>("clean") {
        doLastWith(fileSystemOperations, rootBuildDir, distDir, mediaDir) { fsOps, rootBuildDir, distDir, mediaDir ->
            fsOps.delete { delete(rootBuildDir, distDir, mediaDir) }
        }
    }

    val distTemplate by registering(Copy::class) {
        into(distDir)
        from(distTemplateDir)
    }

    val distProperties by registering(Copy::class) {
        into(file("$distDir/resources"))
        from(file("$distTemplateDir/config"))
        include("application.yaml")
        rename("application.yaml", "application.yaml-default")
    }

    val dist by registering {
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

    val media by registering {
        group = "distribution"
        description = "Cleans, builds the distribution and generates the installer media"
        dependsOn(clean, dist, ":installer:media")
    }

    register("release") {
        group = "release"
        description = "Builds the media and publishes a release"
        dependsOn(clean, media, ":installer:release")
    }

    register("overwriteRelease") {
        group = "release"
        description = "Builds the media and overwrites the existing release"
        dependsOn(clean, media, ":installer:overwriteRelease")
    }

    register("beta") {
        group = "release"
        description = "Builds the media for a beta"
        dependsOn(clean, media)
    }

    register<RunOnGithub>("releaseGithub") {
        group = "release"
        description = "Triggers the release build on GitHub Actions"
    }

    register<RunOnGithub>("betaGithub") {
        group = "release"
        description = "Triggers the beta build on GitHub Actions"
    }

    register("supportBot") {
        group = "help"
        description = "Triggers the support-bot GitHub workflow"
        doLast {
            RunOnGithub.triggerWorkflow(259381811)
        }
    }
}
