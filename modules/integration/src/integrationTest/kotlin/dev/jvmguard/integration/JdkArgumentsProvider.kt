package dev.jvmguard.integration

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.support.ParameterDeclarations
import java.util.stream.Stream
import kotlin.jvm.optionals.getOrNull

/** Supplies one [JdkUnderTest] per requested JDK that the test supports according to [MinJdk]. */
class JdkArgumentsProvider : ArgumentsProvider {

    override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
        val min = context.testClass.getOrNull()?.getAnnotation(MinJdk::class.java)?.value ?: 8
        return requestedJdks()
            .filter { it >= min }
            .map { version ->
                val javaExecutable = System.getProperty("jvmguard.integration.jdkHome.$version")
                    ?: throw IllegalStateException(
                        "no java launcher for JDK $version (system property jvmguard.integration.jdkHome.$version not set)")
                Arguments.of(JdkUnderTest(version, javaExecutable))
            }
            .stream()
    }

    companion object {
        private fun requestedJdks(): List<Int> =
            System.getProperty("jvmguard.integration.jdks", "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.toInt() }
    }
}
