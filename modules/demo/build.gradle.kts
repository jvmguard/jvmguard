import dev.jvmguard.build.*

plugins {
    id("java-module")
}

javaVersion = "21"

dependencies {
    api(project(":agent:api"))
}

tasks {
    classes {
        if (isIdeaRunActive()) {
            println("Configuring Demo for IDE run")
            dependsOn(":agent:bootstrap:dist")
        }
    }

    val jar = named<Jar>("jar")
    projectsEvaluated(jar) {
        getDependencyProjects().forEach { dependsOn(it.tasks.classes) }
        from(getDependencyOutputPaths())
    }

    val dist = register<Sync>("dist") {
        dependsOn(jar)
        into(file("$distDir/lib/demo"))
        from(jar)
    }

    projectsEvaluated(dist) {
        from(getDependencyLibraries())
    }
}
