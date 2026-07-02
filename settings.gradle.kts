pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
            content {
                includeGroup("com.github.ingokegel")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "jvmguard"

// Each project's directory mirrors its path under modules/ (e.g. :backend:collector -> modules/backend/collector).
listOf(
    // agent domain (Java 8, loads into monitored JVMs)
    ":agent",                   // container
    ":agent:bundle",            // aggregate: api-bundle + relocated agent fat jar
    ":agent:core", ":agent:java11", ":agent:bootstrap",
    ":agent:api", ":agent:mbean",
    // backend (Kotlin / JDK 25)
    ":backend", ":backend:data", ":backend:collector", ":backend:connector", ":backend:rest",
    // frontend + app
    ":ui", ":server",
    // tooling / distribution / sites
    ":demo", ":installer", ":docs", ":website",
    // agent integration tests
    ":integration",
    ":integration:workloads", ":integration:workloads:java21", ":integration:workloads:logging",
).forEach { path ->
    include(path)
    project(path).projectDir = file("modules/${path.removePrefix(":").replace(':', '/')}")
}
