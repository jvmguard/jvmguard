package com.jvmguard.ui.views.settings

import com.jvmguard.common.JvmGuardConfig
import com.jvmguard.common.JvmGuardProperties
import com.jvmguard.connector.server.mock.MockServerConnectionImpl
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AgentGuardrailsViewTest : JvmGuardBrowserlessTest() {

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
        // Restore the shared MCP-enabled default
        JvmGuardConfig.setProperties(JvmGuardProperties())
    }

    private fun checkbox(testId: String): Checkbox = find<Checkbox>().all().first { it.testId == testId }
    private fun integerField(testId: String): IntegerField = find<IntegerField>().all().first { it.testId == testId }
    private fun allowedIps(): TextField = find<TextField>().all().first { it.testId == AgentGuardrailsView.ID_ALLOWED_IPS }
    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }

    private val toolControlIds = listOf(
        AgentGuardrailsView.ID_ALLOW_HEAP_DUMP,
        AgentGuardrailsView.ID_ALLOW_JPS,
        AgentGuardrailsView.ID_ALLOW_JFR,
        AgentGuardrailsView.ID_ALLOW_RUN_GC,
    )

    @Test
    fun readOnlyModeDisablesToolTogglesButKeepsSelection() {
        UI.getCurrent().navigate(AgentGuardrailsView::class.java)

        toolControlIds.forEach { assertTrue(checkbox(it).isEnabled, "$it is enabled by default") }
        assertTrue(integerField(AgentGuardrailsView.ID_MAX_RECORDING).isEnabled)
        assertTrue(integerField(AgentGuardrailsView.ID_CAPTURE_COOLDOWN).isEnabled)

        checkbox(AgentGuardrailsView.ID_MCP_READ_ONLY).value = true

        toolControlIds.forEach { assertFalse(checkbox(it).isEnabled, "$it is disabled in read-only mode") }
        assertFalse(integerField(AgentGuardrailsView.ID_MAX_RECORDING).isEnabled)
        assertFalse(integerField(AgentGuardrailsView.ID_CAPTURE_COOLDOWN).isEnabled)
        assertTrue(checkbox(AgentGuardrailsView.ID_ALLOW_HEAP_DUMP).value, "the selection is kept while disabled")
        assertTrue(allowedIps().isEnabled, "the IP allowlist stays editable in read-only mode")

        checkbox(AgentGuardrailsView.ID_MCP_READ_ONLY).value = false
        toolControlIds.forEach { assertTrue(checkbox(it).isEnabled, "$it is re-enabled when read-only is turned off") }
    }

    @Test
    fun runGcTogglePersistsOnSave() {
        UI.getCurrent().navigate(AgentGuardrailsView::class.java)

        checkbox(AgentGuardrailsView.ID_ALLOW_RUN_GC).value = false
        use(shellSave()).click()

        assertFalse(connection.getGlobalConfig(false).guardrailConfig.allowRunGc)
    }

    @Test
    fun mcpDisabledInConfigDisablesEverythingButTheIpAllowlist() {
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { isMcpEnabled = false })
        UI.getCurrent().navigate(AgentGuardrailsView::class.java)

        assertFalse(checkbox(AgentGuardrailsView.ID_MCP_READ_ONLY).isEnabled)
        toolControlIds.forEach { assertFalse(checkbox(it).isEnabled, "$it is disabled when MCP is off") }
        assertFalse(integerField(AgentGuardrailsView.ID_MAX_RECORDING).isEnabled)
        assertFalse(integerField(AgentGuardrailsView.ID_CAPTURE_COOLDOWN).isEnabled)
        assertTrue(allowedIps().isEnabled, "the IP allowlist still guards the REST API when only MCP is off")
    }

    @Test
    fun ipAllowlistIsDisabledOnlyWhenBothMcpAndRestAreOff() {
        JvmGuardConfig.setProperties(JvmGuardProperties().apply { isMcpEnabled = false; isRestApiEnabled = false })
        UI.getCurrent().navigate(AgentGuardrailsView::class.java)

        assertFalse(allowedIps().isEnabled, "the IP allowlist has nothing to guard when both servers are off")
    }
}
