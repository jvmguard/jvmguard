plugins {
    id("kotlin-module")
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
    id("spring-module")
}

noArg {
    annotation("jakarta.xml.bind.annotation.XmlRootElement")
}

dependencies {
    api(project(":backend:collector"))
    api(project(":backend:data"))
    api("jakarta.xml.bind:jakarta.xml.bind-api")
    api("tools.jackson.core:jackson-databind")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
