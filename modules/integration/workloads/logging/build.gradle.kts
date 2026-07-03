import com.jvmguard.build.*

plugins {
    id("kotlin-module")
}

java {
    disableAutoTargetJvm()
}

classFileVersion = "1.8"

dependencies {
    api(libs.bundles.annotations)
    api(project(":agent:api"))
    api(project(":integration:workloads"))
    compileOnly(project(":agent:bundle"))
    implementation("org.apache.logging.log4j:log4j-api:2.25.4")
    implementation("org.apache.logging.log4j:log4j-core:2.25.4")
    implementation("ch.qos.reload4j:reload4j:1.2.26") // CVE-patched drop-in fork of log4j 1.2.x
    implementation("ch.qos.logback:logback-classic:1.5.37")
}
