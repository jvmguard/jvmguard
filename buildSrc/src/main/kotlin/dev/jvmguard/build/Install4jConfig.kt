package dev.jvmguard.build

import com.install4j.gradle.Install4jExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

fun Project.configureInstall4j() {
    configure<Install4jExtension> {
        if (project.hasProperty("install4jHome")) {
            installDir.set(file(projectProperty("install4jHome", "")))
        }
        if (Secret.INSTALL4J_LICENSE_KEY.hasValue) {
            license =  Secret.INSTALL4J_LICENSE_KEY.value
        }
        faster = booleanProperty("fasterBuild", false)
        verbose = booleanProperty("verboseBuild", false)
        disableSigning = booleanProperty("disableSigning", false)
        disableBundling = booleanProperty("disableBundling", false)
        if (project.hasProperty("mediaType")) {
            mediaTypes = listOf(property("mediaType").toString())
        }
    }
}
