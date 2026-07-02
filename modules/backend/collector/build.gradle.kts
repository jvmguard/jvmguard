plugins {
    id("kotlin-module")
    kotlin("plugin.spring")
    id("spring-module")
}

dependencies {
    api(libs.bouncycastle)
    api(project(":backend:data"))
    api(project(":agent:bundle"))
    api("org.jooq:jool:0.9.14")
    implementation(libs.fastutil)
}