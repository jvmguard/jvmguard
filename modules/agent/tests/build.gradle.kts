import dev.jvmguard.build.*

plugins {
    id("java-module")
}

dependencies {
    implementation(project(":agent:core"))
    implementation(project(":agent:api"))
    implementation(project(":agent:mbean"))
    implementation(libs.asm.commons)
    addJunit6()
}

tasks.test {
    useJUnitPlatform()
}
