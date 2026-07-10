import com.jvmguard.build.*

plugins {
    id("kotlin-module")
    kotlin("plugin.spring")
    id("spring-module")
}

val jakartaActivationConfiguration = configurations.detachedConfiguration(
    dependencies.create("jakarta.activation:jakarta.activation-api:2.1.4")
)

dependencies {
    api("org.eclipse.angus:angus-mail:2.0.5") {
        exclude(group = "jakarta.activation")
    }
    api(files(jakartaActivationConfiguration)) // add jakarta activation like this to also keep the earlier version included by jersey in the javax namespace
    api("com.fasterxml.woodstox:woodstox-core:7.1.1")
    api("net.java.dev.stax-utils:stax-utils:20070216") {
        exclude("com.bea.xml")
    }
    api(libs.jdom)
    api(libs.commons.compress)
    api(libs.install4j.runtime)
    api(libs.bundles.kotlin)
    api(project(":agent:bundle"))
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("tools.jackson.core:jackson-core")
    api("tools.jackson.core:jackson-databind")
    api("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation(libs.fastutil)
    implementation(libs.nanojson)
    addJunit6()
    testImplementation(libs.h2)
}

tasks.test {
    useJUnitPlatform()
}