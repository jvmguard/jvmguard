import dev.jvmguard.build.*

plugins {
    id("java-module")
}

jvmguardJava {
    javaVersion.set("1.8")
}

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
        into("$distDir/agent")
        from(jar)
        into("lib") {
            // distJar is archived as agent-bundle.jar; the runtime loads lib/agent.jar
            from(provider { project(":agent:bundle").tasks.named<Jar>("distJar").get().archiveFile }) {
                rename { "agent.jar" }
            }
        }
        dependsOn(provider { project(":agent:bundle").tasks.named("distJar") })
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
