import com.vaadin.gradle.plugin.VaadinPlugin

plugins {
    `java`
}

val vaadinVersion: String = requireNotNull(VaadinPlugin::class.java.`package`?.implementationVersion) {
    "Vaadin implementation version is missing"
}

dependencies {
    implementation(platform("com.vaadin:vaadin-bom:$vaadinVersion"))
}

// Spring Boot's "developmentOnly" configuration does not inherit the platform declared on "implementation",
// so version-less Vaadin dev dependencies fail to resolve.
pluginManager.withPlugin("org.springframework.boot") {
    dependencies {
        add("developmentOnly", platform("com.vaadin:vaadin-bom:$vaadinVersion"))
    }
}

