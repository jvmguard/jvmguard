import dev.jvmguard.build.*

plugins {
    id("kotlin-module")
    kotlin("plugin.spring")
    id("spring-module")
}

dependencies {
    api(libs.h2)
    api(project(":backend:collector"))
    api(project(":backend:data"))
    api("com.google.zxing:javase:3.5.4")
    api("com.unboundid:unboundid-ldapsdk:7.0.4")
    addJunit6()
    testImplementation(libs.h2)
}

tasks.test {
    useJUnitPlatform()
}