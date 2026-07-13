package com.jvmguard.build

import com.install4j.gradle.Install4jExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

fun Project.configureInstall4j() {
    val dev = hasProperty("dev")

    configure<Install4jExtension> {
        if (project.hasProperty("install4jHome")) {
            installDir.set(file(projectProperty("install4jHome", "")))
        }
        if (Secret.INSTALL4J_LICENSE_KEY.hasValue) {
            license =  Secret.INSTALL4J_LICENSE_KEY.value
        }
        faster = booleanProperty("faster", false) || dev
        disableSigning = booleanProperty("disableSigning", false) || dev
        disableBundling = booleanProperty("disableBundling", false) || dev
        if (project.hasProperty("verbose")) {
            verbose = true
        }
        if (project.hasProperty("disableNotarization")) {
            disableNotarization = true
        }

        if (dev) {
            mediaTypes.set(if (project.hasProperty("type")) {
                listOf(property("type").toString())
            } else {
                listOf(
                    getPlatformDescriptor() + when {
                        isMacos() -> "Archive"
                        !isWindows() -> "Installer"
                        else -> ""
                    }
                )
            })
        }
    }
}
