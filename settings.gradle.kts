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
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "jvmguard"

listOf(
    // agent
    ":agent",
    ":agent:bundle",
    ":agent:core",
    ":agent:java11",
    ":agent:bootstrap",
    ":agent:api",
    ":agent:mbean",
    // backend
    ":backend",
    ":backend:data",
    ":backend:collector",
    ":backend:connector",
    ":backend:rest",
    ":backend:mcp",
    // frontend
    ":ui",
    // aggregate server
    ":server",
    // distribution
    ":demo",
    ":installer",
    ":docs",
    ":website",
    // agent integration tests
    ":integration",
    ":integration:workloads",
    ":integration:workloads:java21",
    ":integration:workloads:logging",
    ).forEach { path ->
    include(path)
    project(path).projectDir = file("modules/${path.removePrefix(":").replace(':', '/')}")
}
