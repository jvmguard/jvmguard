import dev.jvmguard.build.*

val npmBin = if (isWindows()) "npm.cmd" else "npm"

// Screenshot locales: "en" maps to the flat images/ui/, the others toimages/ui/generated/<locale>/
val screenshotLocales = listOf("en", "ko", "ja", "zh-CN")

tasks {

    val copyScreenshots = register<Copy>("copyScreenshots") {
        group = "docs"
        mustRunAfter(":ui:screenshots", ":ui:darkScreenshots")
        screenshotLocales.forEach { locale ->
            val target = if (locale == "en") "ui" else "ui/generated/$locale"
            from(project(":ui").layout.buildDirectory.dir("e2e/screenshotsLight/$locale")) { into(target) }
            from(project(":ui").layout.buildDirectory.dir("e2e/screenshotsDark/$locale")) { into(target) }
        }
        into(layout.projectDirectory.dir("public/images"))
    }

    val npmInstall = register<Exec>("npmInstall") {
        group = "docs"
        workingDir = projectDir
        commandLine(npmBin, "ci")
        inputs.file("package.json")
        inputs.file("package-lock.json")
        outputs.dir("node_modules")
    }

    val npmBuild = register<Exec>("npmBuild") {
        group = "docs"
        description = "Builds the Starlight static site into dist/."
        dependsOn(npmInstall)
        workingDir = projectDir
        commandLine(npmBin, "run", "build")
    }

    register<Exec>("npmDev") {
        group = "docs"
        description = "Starts the Starlight dev server"
        dependsOn(npmInstall)
        workingDir = projectDir
        commandLine(npmBin, "run", "dev")
    }

    register<Exec>("npmPreview") {
        group = "docs"
        description = "Serves the built site locally"
        dependsOn(npmBuild)
        workingDir = projectDir
        commandLine(npmBin, "run", "preview")
    }

    register("buildDocs") {
        group = "docs"
        description = "Copies screenshots and builds the site."
        dependsOn(copyScreenshots, npmBuild)
    }
}

