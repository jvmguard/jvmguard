import dev.jvmguard.build.*

plugins {
    id("java-module")
}

dependencies {
    implementation(project(":agent:core"))
    implementation(project(":agent:api"))
    implementation(libs.asm.commons)
    addJunit6()
}

tasks.test {
    useJUnitPlatform()
}
