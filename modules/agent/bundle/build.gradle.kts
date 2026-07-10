import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jvmguard.build.*
import java.io.IOException

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

    val distJar = register<ShadowJar>("distJar") {
        archiveBaseName.set("agent-bundle")
    }

    projectsEvaluated(distJar) {
        val agentModules = getDependencyProjects().filter { it.path != project.path }
        agentModules.forEach { dependsOn(it.tasks.named("jar")) }

        from(getOutputPaths(agentModules))

        manifest {
            attributes("Build-Version" to getBuildVersion())
        }

        configurations.add(shadow)
        minimize {
            exclude(dependency("com.jprofiler:jprofiler-controller:.*"))
        }
        exclude("module-info.class")
        // Relocate bundled libraries so they cannot collide with the profiled application's own copies
        // when the agent is attached to the same JVM.
        relocate("it.unimi.dsi.fastutil", "com.${project.rootProject.name}.agent.fastutil")
        relocate("org.objectweb.asm", "com.jvmguard.agent.asm")
        relocate("com.grack.nanojson", "com.jvmguard.agent.json")
        relocate("com.jprofiler.api.controller", "com.jvmguard.agent.jpcontroller")
    }

    // Pass -PvmName to set the name of the VM, -PvmGroup to set the group of the VM
    val runTest = register<JavaExec>("runTest") {
        dependsOn("testClasses", distJar)
        mainClass.set("javaagent.TestMain")
    }

    projectsEvaluated(runTest) {
        jvmArgs(
            "-javaagent:${project(":agent:bootstrap").tasks.named<Jar>("jar").flatMap { it.archiveFile }.get().asFile}=name=" +
                    project.projectProperty("vmName", "testVM") +
                    (project.projectPropertyOrNull<String>("vmGroup")?.let { ",group=$it" } ?: "")
        )
        classpath = project(":agent:core").the<JavaPluginExtension>().sourceSets["test"].output.classesDirs
    }
}

fun getBuildVersion(): Long {
    val versionParts = getProductVersion("jvmguard").split(".")
    val major = versionParts[0].toLong()
    val minor = versionParts[1].toLong()
    if (minor >= 10) {
        throw RuntimeException("minor version must be < 10")
    }
    val revisionNumber = try {
        getCommittedRevisionNumber()
    } catch (e: IOException) {
        if (isIdeaActive()) 0L else throw e
    }
    return major * 10000000 + minor * 1000000 + revisionNumber
}
