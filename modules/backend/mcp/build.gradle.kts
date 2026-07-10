import com.jvmguard.build.addJunit6

plugins {
    id("kotlin-module")
    kotlin("plugin.spring")
    id("spring-module")
}

dependencies {
    api(project(":backend:connector"))
    api(project(":backend:data"))
    implementation(libs.mcp.sdk)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    compileOnly(libs.servlet.api)
    compileOnly(libs.bundles.annotations)
    addJunit6()
}

tasks.test {
    useJUnitPlatform()
}
