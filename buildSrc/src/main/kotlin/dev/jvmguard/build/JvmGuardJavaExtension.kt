package dev.jvmguard.build

import org.gradle.api.provider.Property

abstract class JvmGuardJavaExtension {
    abstract val javaVersion: Property<String>
    abstract val classFileVersion: Property<String>
}
