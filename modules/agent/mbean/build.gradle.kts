import com.jvmguard.build.*

plugins {
    id("java-module")
}

javaVersion = "1.8"

dependencies {
    api(libs.bundles.annotations)
    implementation(libs.fastutil)
}
