package com.jvmguard.build

import org.apache.tools.ant.filters.FixCrLfFilter
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import java.util.Calendar

fun Jar.addApacheLicense() {
    val licenseFile = project.rootProject.file("buildSrc/apache-license-notice.txt")
    into("META-INF") {
        from(licenseFile) {
            rename { "COPYRIGHT.txt" }
            filter { replaceLicenseNotice(it, "JAR file") }
        }
    }
    val licenseComment by lazy { getJavaComment(replaceLicenseNotice(licenseFile.readText())) }
    filesMatching("**/*.java") {
        var firstLine = true
        filter {
            if (firstLine) {
                firstLine = false
                licenseComment + it
            } else {
                it
            }
        }
        filter(mapOf("eol" to FixCrLfFilter.CrLf.newInstance("lf")), FixCrLfFilter::class.java)
    }
}

private fun replaceLicenseNotice(str: String, fileValue: String = "file"): String =
    str.replace("%YEAR%", Calendar.getInstance().get(Calendar.YEAR).toString()).replace("%FILE%", fileValue)

private fun getJavaComment(str: String): String =
    "/*\n * " + str.lines().joinToString("\n * ") + "\n */\n"
