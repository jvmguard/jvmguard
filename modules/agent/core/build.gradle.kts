import dev.jvmguard.build.*

plugins {
    id("java-module")
}

javaVersion = "1.8"

java {
    disableAutoTargetJvm()
}

dependencies {
    compileOnly(project(":agent:mbean"))
    api(project(":agent:api"))
    implementation(project(":agent:java11"))
    api(libs.bundles.annotations)
    implementation(libs.jprofiler.controller)
    implementation(libs.asm.commons)
    implementation(libs.nanojson)
    implementation(libs.fastutil)
}
