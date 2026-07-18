import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.jvmguard.build.*

plugins {
    id("java-module")
}

java {
    disableAutoTargetJvm()
}

dependencies {
    api(project(":agent:mbean"))
    api(project(":agent:core"))
    api(project(":agent:java11"))
    api(project(":agent:api"))
}

tasks {
    val shadow = configurations.create("shadow")
    dependencies {
        "shadow"(libs.fastutil)
        "shadow"(libs.asm.commons)
        "shadow"(libs.nanojson)
        "shadow"(libs.jprofiler.controller)
    }

    register<ShadowJar>("distJar") {
        archiveBaseName.set("agent-bundle")

        dependsOn(provider { getDependencyProjects().filter { it.path != project.path }.map { it.tasks.named("jar") } })
        from(provider { getOutputPaths(getDependencyProjects().filter { it.path != project.path }) })

        val buildVersion = getBuildVersionProvider()
        inputs.property("buildVersion", buildVersion)
        doFirst {
            manifest.attributes("Build-Version" to buildVersion.get())
        }

        configurations.add(shadow)
        minimize {
            exclude(dependency("com.jprofiler:jprofiler-controller:.*"))
        }
        exclude("module-info.class")
        // Relocate bundled libraries so they cannot collide with the profiled application's own copies
        // when the agent is attached to the same JVM.
        relocate("it.unimi.dsi.fastutil", "com.${project.rootProject.name}.agent.fastutil")
        relocate("org.objectweb.asm", "dev.jvmguard.agent.asm")
        relocate("com.grack.nanojson", "dev.jvmguard.agent.json")
        relocate("com.jprofiler.api.controller", "dev.jvmguard.agent.jpcontroller")
    }
}

