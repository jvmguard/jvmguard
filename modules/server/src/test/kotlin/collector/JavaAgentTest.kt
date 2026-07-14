package collector

import dev.jvmguard.collector.main.VmManagerImpl
import dev.jvmguard.data.config.triggers.TimeUnit
import dev.jvmguard.data.config.triggers.actions.RecordJpsAction
import dev.jvmguard.server.ServerMain
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@Disabled("Manual smoke test: boots a full ServerMain and waits on live agent connections")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavaAgentTest {

    private lateinit var vmManager: VmManagerImpl

    @BeforeAll
    fun setUp() {
        ServerMain.main(emptyArray())
        vmManager = ServerMain.getBean(VmManagerImpl::class.java)
    }

    @Test
    fun connect() {
        val connections = vmManager.currentConnections
        println(connections)
        if (connections.isNotEmpty()) {
            RecordJpsAction().apply {
                time = 5
                timeUnit = TimeUnit.SECONDS
            }

            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}
