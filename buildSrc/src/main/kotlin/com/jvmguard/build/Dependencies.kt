package com.jvmguard.build

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.*

private val Project.catalog get() = the<VersionCatalogsExtension>().named("libs")
private fun Project.version(alias: String): String = catalog.findVersion(alias).get().requiredVersion

fun Project.addJunit6() {
    dependencies {
        add("testImplementation", platform("org.junit:junit-bom:${version("junitBom")}"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }
}
