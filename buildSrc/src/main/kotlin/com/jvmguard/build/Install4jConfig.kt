package com.jvmguard.build

import com.install4j.gradle.Install4jExtension
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

fun Project.getNotarizationKeyFile(): File {
    return layout.buildDirectory.file("notarization-key.p8").get().asFile
}

fun Project.provideNotarizationKey(task: TaskProvider<*>) {
    val notarizationKeyFile = getNotarizationKeyFile()

    val cleanupTask = tasks.register("${task.name}NotarizationCleanup") {
        doFirst {
            notarizationKeyFile.delete()
        }
    }

    task.configure {
        doFirst {
            notarizationKeyFile.parentFile.mkdirs()
            if (isWindows()) {
                Files.createFile(notarizationKeyFile.toPath())
            } else {
                Files.createFile(notarizationKeyFile.toPath(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
            }
            notarizationKeyFile.writeText(Secret.NOTARIZATION_KEY.value)
        }
        finalizedBy(cleanupTask)
    }
}

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
