import com.jvmguard.build.*
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.JavadocMemberLevel
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

plugins {
    id("java-module")
    id("com.vanniktech.maven.publish")
}

interface PublishedApiExtension {
    val artifactName: Property<String>
    val artifactId: Property<String>
    val description: Property<String>
    val automaticModuleName: Property<String>
}

val publishedApi = extensions.create<PublishedApiExtension>("publishedApi")

javaVersion = "1.8"

afterEvaluate {
    tasks.withType<AbstractPublishToMaven> {
        dependsOn(tasks.withType<Sign>())
    }
    configure<MavenPublishBaseExtension> {
        coordinates("com.jvmguard", publishedApi.artifactId.get(), getProductVersion("jvmguard") + getPublishSuffix())
        publishToMavenCentral(automaticRelease = booleanProperty("automaticRelease", true))
        signAllPublications()

        pom {
            name = publishedApi.artifactName.get()
            description = publishedApi.description.get()
            url = "https://jvmguard.dev"

            scm {
                connection = "https://github.com/jvmguard/jvmguard"
                developerConnection = "https://github.com/jvmguard/jvmguard"
                url = "https://github.com/jvmguard/jvmguard"
            }
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    name = "Ingo Kegel"
                    url = "https://github.com/ingokegel"
                    organization = "jvmguard"
                    organizationUrl = "https://github.com/jvmguard"
                }
            }
            configure<SigningExtension> {
                val key = Secret.MAVEN_CENTRAL.resolveComponentInConfigurationPhase("signingInMemoryKey", providers, optional = true)
                val pw = Secret.MAVEN_CENTRAL.resolveComponentInConfigurationPhase("signingInMemoryKeyPassword", providers, optional = true)
                useInMemoryPgpKeys(key, pw)
            }
        }
    }
}

tasks {
    val jar = named<Jar>("jar") {
        archiveBaseName.set(publishedApi.artifactId)
        manifest {
            attributes("Automatic-Module-Name" to publishedApi.automaticModuleName)
        }
        addApacheLicense()
    }

    val sourcesJar = register<Jar>("sourcesJar") {
        archiveBaseName.set(jar.flatMap { it.archiveBaseName })
        from("src")
        archiveClassifier.set("sources")
        addApacheLicense()
    }

    val javadoc = named<Javadoc>("javadoc") {
        val mainSourceSet = project.the<JavaPluginExtension>().sourceSets["main"]
        classpath = mainSourceSet.compileClasspath + mainSourceSet.output
    }

    projectsEvaluated(javadoc) {
        val productName = publishedApi.artifactName.get()
        options {
            this as StandardJavadocDocletOptions
            memberLevel = JavadocMemberLevel.PROTECTED
            header = "$productName API"
            docTitle = "$productName API documentation"
            windowTitle = "$productName API"
            tags = listOf("noinspection")
            jFlags = listOf("-Duser.language=en", "-Duser.country=US")
            // single string options do not work in Javadoc option files, for -Xdoclint
            // add changes to the second argument
            @Suppress("SpellCheckingInspection")
            addStringOption("Xdoclint:all", "-Xdoclint:-missing")
        }
        executable = project.getJavadocExecutable()
    }

    val javadocJar = register<Jar>("javadocJar") {
        dependsOn(javadoc)
        archiveBaseName.set(jar.flatMap { it.archiveBaseName })
        archiveClassifier.set("javadoc")
    }

    projectsEvaluated(javadocJar) {
        from(javadoc.get().destinationDir)
    }

    val copyDist = register<Copy>("copyDist") {
        dependsOn(jar, sourcesJar)
        into(distDir)
        into("api") {
            from(jar.flatMap { it.archiveFile })
            from(sourcesJar.flatMap { it.archiveFile })
        }
    }

    register("dist") {
        dependsOn(copyDist)
    }
}

// "-SNAPSHOT" for beta/snapshot builds, "" otherwise
fun Project.getPublishSuffix(): String {
    val taskNames = gradle.startParameter.taskNames
    return if (taskNames.any { it.contains("publishSnapshot") }  || taskNames.contains("beta")) "-SNAPSHOT" else ""
}
