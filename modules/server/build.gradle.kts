import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jvmguard.build.*

plugins {
    id("kotlin-module")
    kotlin("plugin.spring")
    id("spring-module")
    id("vaadin-bom")
    id("org.springframework.boot")
    // Spring Boot embeds a CycloneDX SBOM in the jar (META-INF/sbom) when this plugin is applied.
    id("org.cyclonedx.bom")
}

dependencies {
    api(project(":backend:collector"))
    api(project(":backend:data"))
    api(project(":backend:connector"))
    api(project(":backend:rest"))
    implementation(project(":ui"))
    implementation("com.vaadin:vaadin-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-tomcat")
    implementation("org.springframework.boot:spring-boot-flyway")
    api(project(":agent:mbean"))
    api(libs.h2)
    implementation(libs.flyway.core)
    addJunit6()
    testImplementation("com.github.dasniko:testcontainers-keycloak:3.7.0")
    testImplementation("org.testcontainers:junit-jupiter:1.21.0")
    developmentOnly("com.vaadin:vaadin-dev")
}

springBoot {
    mainClass = "com.jvmguard.server.ServerMain"
}

// The cyclonedx plugin attaches the SBOM artifact to its consumable "cyclonedxDirectBom" configuration only
// when that task is first realized. Realize the task now, so the artifact is attached before any consumer observes the variant.
afterEvaluate {
    tasks.named("cyclonedxDirectBom").get()
}

tasks {

    test {
        useJUnitPlatform()
    }

    // Keep bootRun's runtime files (data dir, embedded H2 DB, logs, generated keystores) under the
    // build directory rather than polluting modules/server/.
    named<JavaExec>("bootRun") {
        val runDir = layout.buildDirectory.dir("jvmguard-run").get().asFile
        doFirst { runDir.mkdirs() }
        workingDir = runDir
    }


    classes {
        if (System.getProperty("idea.active") == "true" && System.getProperty("idea.sync.active") != "true") {
            println("Configuring for IDE run")
            dependsOn(":ui:vaadinBuildFrontend")
        }
    }

    val jar = named<Jar>("jar")

    val serverJar = register<ShadowJar>("serverJar") {
        dependsOn(jar)
        from(zipTree(jar.flatMap { it.archiveFile }))
        archiveAppendix.set("server")
        archiveBaseName.set("jvmguard")
    }

    projectsEvaluated(serverJar) {
        getDependencyProjects().forEach { dependsOn(it.tasks.classes) }
        from(getDependencyOutputPaths())
    }

    val dist = register<Copy>("dist") {
        into(file("$distDir/lib/server").apply { mkdirs() })
        from(serverJar.get().archiveFile)
    }

    projectsEvaluated(dist) {
        from(getDependencyLibraries())
        from(project(":ui").tasks.named<Jar>("jar").flatMap { it.archiveFile })
    }
}
