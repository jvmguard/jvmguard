plugins {
    id("java-module")
}

jvmguardJava {
    javaVersion.set("1.8")
}

dependencies {
    api(libs.bundles.annotations)
    implementation(libs.fastutil)
}
