package dev.jvmguard.ui.views.settings

import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.common.JvmGuardProperties
import dev.jvmguard.connector.server.mock.MockServerConnectionImpl
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NetworkViewTest : JvmGuardBrowserlessTest() {

    private lateinit var connection: MockServerConnectionImpl

    @BeforeEach
    fun setUp() {
        connection = MockConnections.create(AccessLevel.ADMIN)
        Sessions.setCurrent(UserSession(connection))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.clearSettingsDraft()
        JvmGuardConfig.setProperties(JvmGuardProperties())
    }

    private fun readOnly(): Checkbox = find<Checkbox>().all().first { it.testId == NetworkView.ID_MCP_READ_ONLY }
    private fun allowedIps(): TextField = find<TextField>().all().first { it.testId == NetworkView.ID_ALLOWED_IPS }
    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }

    @Test
    fun readOnlyModePersistsOnSave() {
        UI.getCurrent().navigate(NetworkView::class.java)

        readOnly().value = true
        use(shellSave()).click()

        assertTrue(connection.getGlobalConfig(false).guardrailConfig.mcpReadOnly)
    }

    @Test
    fun readOnlyIsDisabledWhenMcpIsOffButTheAllowlistStillGuardsRest() {
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { isMcpEnabled = false })
        UI.getCurrent().navigate(NetworkView::class.java)

        assertFalse(readOnly().isEnabled)
        assertTrue(allowedIps().isEnabled, "the IP allowlist still guards the REST API")
    }

    @Test
    fun ipAllowlistIsDisabledOnlyWhenBothMcpAndRestAreOff() {
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { isMcpEnabled = false; isRestApiEnabled = false })
        UI.getCurrent().navigate(NetworkView::class.java)

        assertFalse(allowedIps().isEnabled)
    }
}
