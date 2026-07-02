import com.jvmguard.build.*

val npmBin = if (isWindows()) "npm.cmd" else "npm"

tasks {

    val copyScreenshots by registering(Copy::class) {
        group = "docs"
        from(project(":ui").layout.buildDirectory.dir("e2e/screenshotsLight")) { into("ui") }
        from(project(":ui").layout.buildDirectory.dir("e2e/screenshotsDark")) { into("ui") }
        into(layout.projectDirectory.dir("public/images"))
    }

    val npmInstall by registering(Exec::class) {
        group = "docs"
        workingDir = projectDir
        commandLine(npmBin, "ci")
        inputs.file("package.json")
        inputs.file("package-lock.json")
        outputs.dir("node_modules")
    }

    val npmBuild by registering(Exec::class) {
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

