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
    api(libs.kover.gradle.plugin)

    // TODO check if this can be removed after the next cyclonedx release
    api(platform(libs.jackson.bom))
}
