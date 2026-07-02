import com.jvmguard.build.*
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("published-api-module")
}

publishedApi {
    artifactName = "JvmGuard API"
    artifactId = "jvmguard-api"
    description = "JvmGuard API for configuring transactions and telemetries in your own code"
    automaticModuleName = "com.jvmguard.api"
}

tasks {
    val transformOverview by registering(Copy::class) {
        from(file("overview.html"))
        into(file("$buildDirFile/overview").apply { mkdirs() })
        filter(mapOf("tokens" to mapOf(
                "VERSION" to getProductVersion("jvmguard")
        )), ReplaceTokens::class.java)
    }

    val javadoc = named<Javadoc>("javadoc") {
        dependsOn(transformOverview)
        val overviewFile = file("$buildDirFile/overview/overview.html")
        inputs.file(overviewFile)
        options.overview = overviewFile.path
    }

    val distJavadoc by registering(Copy::class) {
        dependsOn(javadoc)
        into(file("$distDir/api/doc").apply { mkdirs() })
        from(javadoc.flatMap { provider { it.destinationDir } })
    }

    "dist" {
        dependsOn(distJavadoc)
    }
}
