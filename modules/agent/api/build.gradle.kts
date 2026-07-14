import dev.jvmguard.build.*
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("published-api-module")
}

publishedApi {
    artifactName = "JvmGuard API"
    artifactId = "jvmguard-annotations"
    description = "JvmGuard API for configuring transactions and telemetries in your own code"
    automaticModuleName = "dev.jvmguard.annotation"
}

tasks {
    val transformOverview = register<Copy>("transformOverview") {
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

    val distJavadoc = register<Copy>("distJavadoc") {
        dependsOn(javadoc)
        into(file("$distDir/api/doc").apply { mkdirs() })
        from(javadoc.flatMap { provider { it.destinationDir } })
    }

    "dist" {
        dependsOn(distJavadoc)
    }
}
