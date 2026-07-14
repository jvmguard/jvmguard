import dev.jvmguard.build.*

val npmBin = if (isWindows()) "npm.cmd" else "npm"

tasks {

    val npmInstall = register<Exec>("npmInstall") {
        group = "website"
        description = "Installs the website npm dependencies (npm ci)."
        workingDir = projectDir
        commandLine(npmBin, "ci")
        inputs.file("package.json")
        inputs.file("package-lock.json")
        outputs.dir("node_modules")
    }

    val npmBuild = register<Exec>("npmBuild") {
        group = "website"
        description = "Builds the marketing site into dist/."
        dependsOn(npmInstall)
        workingDir = projectDir
        commandLine(npmBin, "run", "build")
    }

    register<Exec>("npmDev") {
        group = "website"
        description = "Starts the website dev server."
        dependsOn(npmInstall)
        workingDir = projectDir
        commandLine(npmBin, "run", "dev")
    }

    register<Exec>("npmPreview") {
        group = "website"
        description = "Serves the built website locally."
        dependsOn(npmBuild)
        workingDir = projectDir
        commandLine(npmBin, "run", "preview")
    }

    register("buildWebsite") {
        group = "website"
        description = "Builds the marketing site."
        dependsOn(npmBuild)
    }

    register<Exec>("devAll") {
        group = "website"
        description = "Starts both the website (4321) and docs (4322) dev servers."
        dependsOn(npmInstall)
        workingDir = projectDir
        commandLine(npmBin, "run", "dev:all")
    }
}
