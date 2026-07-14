import dev.jvmguard.build.*

plugins {
    id("kotlin-module")
}

java {
    disableAutoTargetJvm()
}

// Workloads that use Java 21 APIs (virtual threads): compiled with JDK 21, targeting Java 21 class files.
// VThreadManyTest gates them to JDK 21+ at runtime (@MinJdk(21) + isRunOnVM(isAtLeastJava(21))), so 21 is
// the lowest JDK they ever load on (in -Pjdks=all); the test/driver JVM is the 25 baseline.
javaVersion = "21"

dependencies {
    api(libs.bundles.annotations)
    api(project(":agent:api"))
    api(project(":integration:workloads"))
    compileOnly(project(":agent:bundle"))
}
