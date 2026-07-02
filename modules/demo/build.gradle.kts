import com.jvmguard.build.*

plugins {
    id("java-module")
}

javaVersion = "21"

dependencies {
    api(project(":agent:api"))
}

tasks {
    classes {
        if (System.getProperty("idea.active") == "true" && System.getProperty("idea.sync.active") != "true") {
            println("Configuring Demo for IDE run")
            dependsOn(":agent:bootstrap:dist")
        }
    }

    val jar = named<Jar>("jar")
    projectsEvaluated(jar) {
        getDependencyProjects().forEach { dependsOn(it.tasks.classes) }
        from(getDependencyOutputPaths())
    }

    val dist by registering(Copy::class) {
        dependsOn(jar)
        into(file("$distDir/lib/demo").apply { mkdirs() })
        from(jar)
    }

    projectsEvaluated(dist) {
        from(getDependencyLibraries())
    }
}
