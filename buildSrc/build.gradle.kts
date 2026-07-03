plugins {
    `kotlin-dsl`
    `java-library`
}

dependencies {
    api(libs.kotlin.gradle.plugin)
    api(libs.kotlin.allopen.plugin)
    api(libs.kotlin.noarg.plugin)
    api(libs.vanniktech.publish.plugin)
    api(libs.install4j.gradle.plugin)
    api(libs.shadow.gradle.plugin)
    api(libs.json)
    api(libs.cyclonedx.plugin)
    api(libs.spring.boot.gradle.plugin)
    api(libs.vaadin.gradle.plugin)

    // TODO check if this can be removed after the next release of cyclonedx plugin (fixes several CVEs)
    api(platform("com.fasterxml.jackson:jackson-bom:2.22.0"))
}
