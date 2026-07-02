@file:Suppress("AvoidDuplicateDependencies")

plugins {
    id("kotlin-module")
}

java {
    // depends on Java 1.8 workloads
    disableAutoTargetJvm()
}

sourceSets.create("integrationTest")
sourceSets.create("benchmark")

val workloadRuntimeClasspath: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    "integrationTestImplementation"(platform(libs.junit.bom))
    "integrationTestImplementation"("org.junit.jupiter:junit-jupiter")
    "integrationTestRuntimeOnly"("org.junit.platform:junit-platform-launcher")

    // for starting ServerMain in-process
    "integrationTestImplementation"(project(":server"))
    "integrationTestImplementation"(project(":backend:collector"))
    "integrationTestImplementation"(project(":backend:connector"))
    "integrationTestImplementation"(project(":backend:data"))
    "integrationTestImplementation"(project(":agent:bundle"))
    "integrationTestImplementation"(project(":agent:api"))

    // Workload classes
    "integrationTestImplementation"(project(":integration:workloads"))
    "integrationTestImplementation"(project(":integration:workloads:java21"))
    workloadRuntimeClasspath(project(":integration:workloads"))
    workloadRuntimeClasspath(project(":integration:workloads:java21"))
    "integrationTestImplementation"(project(":integration:workloads:logging"))
    workloadRuntimeClasspath(project(":integration:workloads:logging"))

    // test libraries
    "integrationTestImplementation"("com.beust:klaxon:5.6") { exclude(group = "org.jetbrains.kotlin") }
    "integrationTestImplementation"("org.wiremock:wiremock-standalone:3.13.2")
    "integrationTestImplementation"("com.github.davidmoten:subethasmtp:7.2.0") {
        exclude(group = "jakarta.mail")
        exclude(group = "jakarta.activation")
        exclude(group = "org.slf4j")
        exclude(group = "com.google.code.findbugs")
    }
    "integrationTestImplementation"("jakarta.mail:jakarta.mail-api:2.1.5")

    "benchmarkImplementation"(sourceSets["integrationTest"].output)
}

configurations["benchmarkImplementation"].extendsFrom(configurations["integrationTestImplementation"])
configurations["benchmarkRuntimeOnly"].extendsFrom(configurations["integrationTestRuntimeOnly"])

val allTestJdks = listOf(8, 11, 17, 21, 25)

fun Test.configureIntegrationTest() {
    group = "verification"
    val integrationTest = sourceSets["integrationTest"]
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    dependsOn(":agent:bootstrap:dist")

    val jdksSpec = providers.gradleProperty("jdks").orNull
    val requestedJdks = when {
        jdksSpec.isNullOrBlank() -> listOf(JavaVersion.current().majorVersion.toInt())
        jdksSpec == "all" -> allTestJdks
        else -> jdksSpec.split(",").map { it.trim().toInt() }
    }
    val toolchains = project.extensions.getByType(JavaToolchainService::class.java)
    systemProperty("jvmguard.integration.jdks", requestedJdks.joinToString(","))
    requestedJdks.forEach { version ->
        val launcher = toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(version)) }
        systemProperty("jvmguard.integration.jdkHome.$version", launcher.get().executablePath.asFile.absolutePath)
    }

    setForkEvery(1)
    maxParallelForks = 1

    val distDir = rootDir.resolve("dist")
    val workDir = layout.buildDirectory.dir("integration").get().asFile
    systemProperty("jvmguard.integration.distDir", distDir.absolutePath)
    systemProperty("jvmguard.integration.workDir", workDir.absolutePath)
    // For the SSL/STARTTLS mail trigger tests
    systemProperty("test.keyStore", layout.projectDirectory.file("teststore.jks").asFile.absolutePath)
    // The server cannot find the installation layout logback.xml and would fall back to console logging.
    systemProperty("jvmguard.integration.logbackFile", rootDir.resolve("dist-template/logback.xml").absolutePath)
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
    )
    // Regenerate golden data under build/gradle/.../integration/<Test>-<jdk>/output instead of comparing.
    // Copy the regenerated *.xml back over src/integrationTest/resources/.../data/
    val recordSpec = providers.gradleProperty("jvmguard.record").orNull
    if (recordSpec != null && recordSpec != "false") {
        systemProperty("jvmguard.record", "true")
    }
    // The classpath for workloads
    val workloadFiles: FileCollection = files(workloadRuntimeClasspath)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Djvmguard.integration.workloadClasspath=" + workloadFiles.asPath)
    })
    outputs.upToDateWhen { false }
}

tasks.register<Test>("integrationTest") {
    description = "Agent integration tests: real server + agent-instrumented workload JVMs."
    useJUnitPlatform()
    configureIntegrationTest()
}

tasks.register<Test>("citest") {
    description = "The citest subset of the agent integration tests, on the current JDK."
    useJUnitPlatform {
        includeTags("citest")
    }
    configureIntegrationTest()
}

// Standalone throughput benchmark
tasks.register<JavaExec>("perfBenchmark") {
    group = "verification"
    description = "Runs the standalone agent throughput benchmark (BasePerfWorkload) on the current JDK."
    dependsOn(":agent:bootstrap:dist")
    mainClass.set("com.jvmguard.integration.PerfBenchmarkKt")
    classpath = sourceSets["benchmark"].runtimeClasspath

    val distDir = rootDir.resolve("dist")
    val workDir = layout.buildDirectory.dir("benchmark").get().asFile
    systemProperty("jvmguard.integration.distDir", distDir.absolutePath)
    systemProperty("jvmguard.integration.workDir", workDir.absolutePath)
    systemProperty("jvmguard.integration.logbackFile", rootDir.resolve("dist-template/logback.xml").absolutePath)
    providers.gradleProperty("jvmguard.benchmark.repetition").orNull?.let {
        systemProperty("jvmguard.benchmark.repetition", it)
    }
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
    )
    val workloadFiles: FileCollection = files(workloadRuntimeClasspath)
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Djvmguard.integration.workloadClasspath=" + workloadFiles.asPath)
    })
}
