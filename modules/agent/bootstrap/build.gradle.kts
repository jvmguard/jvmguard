import dev.jvmguard.build.*

plugins {
    id("java-module")
}

javaVersion = "1.8"

java {
    disableAutoTargetJvm()
}

dependencies {
    api(project(":agent:bundle"))
}

tasks {
    val jar = named<Jar>("jar") {
        archiveBaseName.set("jvmguard")
        manifest {
            attributes(
                "Premain-Class" to "dev.jvmguard.agent.bootstrap.BootstrapAgent",
                "Agent-Class" to "dev.jvmguard.agent.bootstrap.BootstrapAgent",
                "Main-Class" to "dev.jvmguard.agent.bootstrap.BootstrapMain",
                "Can-Retransform-Classes" to "true",
                "Can-Set-Native-Method-Prefix" to "true"
            )
        }
    }

    val copyDist = register<Copy>("copyDist") {
        dependsOn("jar")
        into(file("$distDir/agent").apply { mkdirs() })
        from(jar)
    }

    projectsEvaluated(copyDist) {
        into("lib") {
            // distJar is archived as agent-bundle.jar; the runtime loads lib/agent.jar
            from(project(":agent:bundle").tasks.named<Jar>("distJar").get().archiveFile) {
                rename { "agent.jar" }
            }
        }
    }

    register("dist") {
        dependsOn(copyDist)
    }

    register<Copy>("prepareTest") {
        dependsOn(jar)
        from("installation.properties", "agent.ks")
        into(jar.get().destinationDirectory)
    }
}
