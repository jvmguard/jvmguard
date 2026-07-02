import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    `java-library`
}

dependencies {
    api(platform(SpringBootPlugin.BOM_COORDINATES))
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.security:spring-security-core")
}
