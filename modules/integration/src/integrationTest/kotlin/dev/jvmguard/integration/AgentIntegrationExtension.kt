package dev.jvmguard.integration

import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/** Wires an [AgentFixture] into each test before it executes; the fixture controls the server and workload lifecycle. */
class AgentIntegrationExtension : BeforeEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        val instance = context.requiredTestInstance
        if (instance is JvmGuardTest) {
            instance.fixture = AgentFixture()
        }
    }
}
