import dev.jvmguard.build.*

plugins {
    id("java-module")
}

jvmguardJava {
    javaVersion.set("21")
}

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
    jar.configure {
        dependsOn(provider { getDependencyProjects().map { it.tasks.named("classes") } })
        from(provider { getDependencyOutputPaths() })
    }

    val dist = register<Sync>("dist") {
        dependsOn(jar)
        into(file("$distDir/lib/demo"))
        from(jar)
        from(getDependencyLibraries())
    }
}
