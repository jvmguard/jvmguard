import dev.jvmguard.build.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files.createTempDirectory
import java.time.Duration

plugins {
    id("kotlin-module")
    id("com.vaadin")
    id("spring-module")
    id("vaadin-bom")
}

// Committed artifact created by `?mock=demo`. Inert unless a `?mock=demo` login occurs
val demoSnapshotFile: File = File(project.projectDir, "src/test/resources/demo-snapshot/demo.json.gz")

val e2ePort = (findProperty("jvmguard.e2e.port") as String?)?.toInt() ?: 8123
val e2eVmPort = (findProperty("jvmguard.e2e.vmPort") as String?)?.toInt() ?: 8948
val e2eServerRuntime = configurations.create("e2eServerRuntime")

val dataPort = (findProperty("jvmguard.data.port") as String?)?.toInt() ?: 8124
val dataVmPort = (findProperty("jvmguard.data.vmPort") as String?)?.toInt() ?: 8949
val dataServer = gradle.sharedServices.registerIfAbsent("jvmguardWebDataServer", ServerProcessService::class.java) {}
val demoCluster = gradle.sharedServices.registerIfAbsent("jvmguardWebDemoCluster", DemoClusterService::class.java) {}
val dataServerDataDir: File by lazy { createTempDirectory("jvmguard-web-data").toFile() }
val demoRuntime = configurations.create("demoRuntime")

java {
    disableAutoTargetJvm()
}

vaadin {
    productionMode = true
}

configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

dependencies {
    api("com.vaadin:vaadin-core")

    implementation(libs.karibu.dsl) {
        exclude(group = "org.hibernate.validator")
        exclude(group = "org.glassfish", module = "jakarta.el")
        exclude(group = "jakarta.el", module = "jakarta.el-api")
        exclude(group = "org.jetbrains.kotlin-wrappers")
    }

    implementation("org.springframework.boot:spring-boot-starter-security")

    compileOnly("com.vaadin:vaadin-spring")

    compileOnly(libs.servlet.api)

    compileOnly("tools.jackson.core:jackson-databind")
    compileOnly(project(":backend:connector"))
    compileOnly(project(":backend:data"))

    implementation(project(":agent:mbean"))

    testImplementation("com.vaadin:browserless-test-junit6")
    testImplementation(libs.playwright)
    testImplementation("com.unboundid:unboundid-ldapsdk:7.0.4")

    e2eServerRuntime(project(":server"))
    e2eServerRuntime(libs.install4j.runtime)

    demoRuntime(project(":demo"))
}

addJunit6()

// Full ServerMain JVM arguments for an E2E run; the launcher prepends the java binary and -cp <cp>.
fun serverMainJvmArgs(port: Int, vmPort: Int, productionMode: Boolean, dataDir: File): List<String> = listOfNotNull(
    "-Xmx512m",
    "-Djvmguard.integrationTest=true",
    "-Djvmguard.dataDirectory=${dataDir.path}",
    "-Djvmguard.httpPort=$port",
    "-Djvmguard.vmPort=$vmPort",
    "-Djvmguard.startH2Console=false",
    "-Djvmguard.restApiEnabled=true",
    "-Djvmguard.restFailedAuthWait=0",
    "-Djvmguard.demoSnapshot=${demoSnapshotFile.absolutePath}",
    if (productionMode) "-Dvaadin.productionMode=true" else null,
    "-Dinstall4j.noProxyAutoDetect=true",
    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
    "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
    "dev.jvmguard.server.ServerMain",
)

val e2eServer = gradle.sharedServices.registerIfAbsent("jvmguardWebServer-e2e", ServerProcessService::class.java) {}
val e2eServerDataDir: File by lazy { createTempDirectory("jvmguard-web-e2e").toFile() }
val e2eServerClasspath = e2eServerRuntime.elements.map { set -> set.joinToString(File.pathSeparator) { it.asFile.absolutePath } }


tasks {

    val dist = register("dist")

    test {
        useJUnitPlatform {
            excludeTags("e2e", "screenshot", "config-e2e", "data-e2e")
        }
        systemProperty("vaadin.productionMode", "true")
    }

    val installPlaywrightBrowsers = register<JavaExec>("installPlaywrightBrowsers") {
        description = "Installs the Chromium browser used by the Playwright e2e tests."
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set("com.microsoft.playwright.CLI")
        args("install", "chromium")
    }

    withType<Test>().configureEach {
        if (name != "test") {
            dependsOn(installPlaywrightBrowsers)
            environment("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")
        }
    }

    val e2eStartServer = register("e2eStartServer") {
        inputs.files(e2eServerRuntime)
        // Localize script-level state so the task action does not capture the build script
        // object (configuration-cache requirement).
        val service = e2eServer
        val port = e2ePort
        usesService(service)
        val command = serverMainJvmArgs(port, e2eVmPort, productionMode = false, dataDir = e2eServerDataDir)
        val serverLog = project.layout.buildDirectory.file("e2e/server.log").get().asFile
        doLastWith(e2eServerClasspath, serverLog) { cp, log ->
            val java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            service.get().start(listOf(java, "-cp", cp.get()) + command, port, "/login", log)
        }
    }
    val e2eStopServer = register("e2eStopServer") {
        usesService(e2eServer)
        doLastWith(e2eServer) { it.get().shutdown() }
    }
    register<Test>("e2eTest") {
        description = "Playwright real-browser smoke against the server on port $e2ePort."
        dependsOn(e2eStartServer)
        finalizedBy(e2eStopServer)
        group = "verification"
        val testSourceSet = sourceSets.test.get()
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        useJUnitPlatform { includeTags("e2e") }
        systemProperty("jvmguard.e2e.url", "http://localhost:$e2ePort")
        systemProperty("jvmguard.e2e.screenshotDir", project.layout.buildDirectory.dir("e2e").get().asFile.path)
        outputs.upToDateWhen { false }
    }

    register<Test>("configE2eTest") {
        description = "Real-server config round-trip E2E tests (@Tag config-e2e)."
        group = "verification"
        dependsOn("e2eStartServer")
        finalizedBy("e2eStopServer")
        val testSourceSet = sourceSets.test.get()
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        useJUnitPlatform {
            includeTags("config-e2e")
        }
        maxParallelForks = 1
        systemProperty("jvmguard.e2e.url", "http://localhost:$e2ePort")
        outputs.upToDateWhen { false }
    }

    val dataE2eStartServer = register("dataE2eStartServer") {
        usesService(dataServer)
        inputs.files(e2eServerRuntime)
        val service = dataServer
        val port = dataPort
        val classpath = e2eServerRuntime.elements.map { set ->
            set.joinToString(File.pathSeparator) { it.asFile.absolutePath }
        }
        val serverLog = layout.buildDirectory.file("data-e2e/server.log").get().asFile
        val command = serverMainJvmArgs(port, dataVmPort, productionMode = false, dataDir = dataServerDataDir)
        doLastWith(classpath, serverLog) { cp, log ->
            val java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            service.get().start(listOf(java, "-cp", cp.get()) + command, port, "/login", log)
        }
    }

    val dataE2eStartDemo = register("dataE2eStartDemo") {
        usesService(demoCluster)
        dependsOn(dataE2eStartServer)
        inputs.files(demoRuntime)
        val service = demoCluster
        val vmPort = dataVmPort
        val classpath = demoRuntime.elements.map { set ->
            set.joinToString(File.pathSeparator) { it.asFile.absolutePath }
        }
        val repoRoot = project.rootDir
        val demoLog = layout.buildDirectory.file("data-e2e/demo.log").get().asFile
        doLastWith(classpath, repoRoot, demoLog) { cp, root, log ->
            service.get().start(listOf(
                System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
                "-cp", cp.get(),
                "-DvmPort=$vmPort",
                "dev.jvmguard.demo.server.JvmGuardDemoServerStarter",
            ), root, log)
        }
    }

    val dataE2eStopDemo = register("dataE2eStopDemo") {
        usesService(demoCluster)
        doLastWith(demoCluster) { service -> service.get().shutdown() }
    }

    val dataE2eStopServer = register("dataE2eStopServer") {
        usesService(dataServer)
        doLastWith(dataServer) { service -> service.get().shutdown() }
    }

    register<Test>("dataE2eTest") {
        description = "Real-data E2E (@Tag data-e2e) against a demo-mode ServerMain on port $dataPort."
        group = "verification"
        dependsOn(dataE2eStartDemo)
        finalizedBy(dataE2eStopDemo, dataE2eStopServer)
        val testSourceSet = sourceSets.test.get()
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        useJUnitPlatform {
            includeTags("data-e2e")
        }
        maxParallelForks = 1
        systemProperty("jvmguard.e2e.url", "http://localhost:$dataPort")
        outputs.upToDateWhen { false }
    }

    register<Test>("screenshots") {
        description = "Produces the help-PDF screenshots into build/e2e/screenshotsLight."
        group = "documentation"
        dependsOn("e2eStartServer")
        finalizedBy("e2eStopServer")
        val testSourceSet = sourceSets.test.get()
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        useJUnitPlatform {
            includeTags("screenshot")
        }
        // One test at a time against the single shared server, so captures never contend (deterministic runs).
        maxParallelForks = 1
        systemProperty("jvmguard.e2e.url", "http://localhost:$e2ePort")
        // Dev mode compiles each route on first hit; be patient so the first navigation never flakes.
        systemProperty("jvmguard.e2e.timeoutMs", "30000")
        systemProperty("jvmguard.e2e.screenshotDir",
            layout.buildDirectory.dir("e2e/screenshotsLight").get().asFile.path)
        outputs.upToDateWhen { false }
    }

    register<Test>("darkScreenshots") {
        description = "Produces the dark-theme help-PDF screenshots into build/e2e/screenshotsDark."
        group = "documentation"
        dependsOn("e2eStartServer")
        finalizedBy("e2eStopServer")
        val testSourceSet = sourceSets.test.get()
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        useJUnitPlatform {
            includeTags("screenshot")
        }
        // One test at a time against the single shared server, so captures never contend (deterministic runs).
        maxParallelForks = 1
        systemProperty("jvmguard.e2e.url", "http://localhost:$e2ePort")
        // Dev mode compiles each route on first hit; be patient so the first navigation never flakes.
        systemProperty("jvmguard.e2e.timeoutMs", "30000")
        systemProperty("jvmguard.e2e.screenshotDir",
            layout.buildDirectory.dir("e2e/screenshotsDark").get().asFile.path)
        systemProperty("jvmguard.e2e.darkScreenshots", "true")
        outputs.upToDateWhen { false }
    }

    register("demoSnapshot") {
        description = "Captures a demo snapshot (?mock=demo) into src/test/resources/demo-snapshot/."
        group = "documentation"
        // Self-contained mode: start the data server + demo cluster, warm, capture, stop.
        // Existing mode (-Pjvmguard.demo.existingUrl=http://host:port): capture against an already-running demo server.
        val existingUrl = (findProperty("jvmguard.demo.existingUrl") as String?)?.takeIf { it.isNotBlank() }
        val warmupSeconds = ((findProperty("jvmguard.demo.warmupSeconds") as String?)?.toLongOrNull() ?: 90L)
        if (existingUrl == null) {
            dependsOn(dataE2eStartDemo)
            usesService(dataServer)
            usesService(demoCluster)
            finalizedBy(dataE2eStopDemo, dataE2eStopServer)
        }
        val port = dataPort
        val target = demoSnapshotFile
        doLast {
            val baseUrl = existingUrl ?: "http://localhost:$port"
            val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
            fun get(path: String): Int = runCatching {
                val req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(60)).GET().build()
                http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode()
            }.getOrDefault(-1)

            require(get("/test?command=ping") < 400) {
                "No server with the test control filter at $baseUrl (start one with -Djvmguard.testControlFilter=true)."
            }
            get("/test?command=createUser") // creates the 'test' admin on a fresh server; no-op-safe otherwise

            if (existingUrl == null && warmupSeconds > 0) {
                logger.lifecycle("Warming up demo cluster for {}s ...", warmupSeconds)
                val end = System.currentTimeMillis() + warmupSeconds * 1000
                while (System.currentTimeMillis() < end) {
                    Thread.sleep(5000)
                }
            }

            val tmp = File(System.getProperty("java.io.tmpdir"), "jvmguard-demo-snapshot.json.gz")
            require(get("/test?command=captureMockSnapshot&path=${tmp.absolutePath}") < 400) { "Capture command failed at $baseUrl" }
            target.parentFile.mkdirs()
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
            logger.lifecycle("Wrote demo snapshot to {}", target.absolutePath)
        }
        outputs.upToDateWhen { false }
    }

    // Vaadin generates frontend/index.html but the plugin does not register it as an input of vaadinBuildFrontend
    named("vaadinBuildFrontend") {
        inputs.files(layout.projectDirectory.file("frontend/index.html")) // FileCollection tolerates a missing entry
            .withPropertyName("frontendIndexHtml")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}

