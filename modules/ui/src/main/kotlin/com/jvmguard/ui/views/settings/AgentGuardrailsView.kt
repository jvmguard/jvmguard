package com.jvmguard.ui.views.settings

import com.jvmguard.common.JvmGuardConfig
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.user.Roles
import com.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/guardrails", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class AgentGuardrailsView : AbstractSettingsSectionView() {

    private val mcpEnabledInConfig = JvmGuardConfig.properties().isMcpEnabled

    private val mcpReadOnly = Checkbox("MCP read-only mode (blocks mutating actions and diagnostic captures)").apply {
        testId = ID_MCP_READ_ONLY
        isEnabled = mcpEnabledInConfig
    }
    private val mcpDisabledNote = Span(
        "The MCP server is disabled in the server configuration (application.yaml), so this setting has no effect."
    ).apply {
        addClassName("jvmguard-field-hint")
        isVisible = !mcpEnabledInConfig
    }

    private val maxRecordingMinutes = IntegerField("Maximum recording duration (minutes, 0 = no limit)").apply {
        testId = ID_MAX_RECORDING
        addClassName("jvmguard-nowrap-label")
        min = 0
        max = 1000
        width = "16rem"
    }
    private val captureCooldown = IntegerField("Minimum seconds between captures on a VM (0 = no limit)").apply {
        testId = ID_CAPTURE_COOLDOWN
        addClassName("jvmguard-nowrap-label")
        min = 0
        width = "16rem"
    }

    private val allowHeapDump = Checkbox("Allow heap dumps").apply { testId = ID_ALLOW_HEAP_DUMP }
    private val allowJps = Checkbox("Allow JProfiler snapshots").apply { testId = ID_ALLOW_JPS }
    private val allowJfr = Checkbox("Allow JFR recordings").apply { testId = ID_ALLOW_JFR }

    private val apiAllowedIps = TextField("Allowed API client IP addresses").apply {
        testId = ID_ALLOWED_IPS
        setWidthFull()
        helperText = "Comma-separated IPv4/IPv6 addresses or CIDR ranges for the MCP server and REST API. Empty allows any address."
    }

    init {
        add(
            settingsSection("MCP access", mcpReadOnly, mcpDisabledNote),
            settingsSection("Diagnostic captures", allowHeapDump, allowJps, allowJfr, maxRecordingMinutes, captureCooldown),
            settingsSection("Network", apiAllowedIps),
        )
    }

    override fun bind(binder: Binder<GlobalConfig>) {
        binder.forField(mcpReadOnly)
            .bind({ it.guardrailConfig.mcpReadOnly }, { config, value -> config.guardrailConfig.mcpReadOnly = value })
        binder.forField(maxRecordingMinutes)
            .asRequired("Enter a number of minutes.")
            .withConverter({ it * 60 }, { it / 60 })
            .bind({ it.guardrailConfig.maxRecordingSeconds }, { config, value -> config.guardrailConfig.maxRecordingSeconds = value })
        binder.forField(captureCooldown)
            .asRequired("Enter a number of seconds.")
            .bind({ it.guardrailConfig.captureCooldownSeconds }, { config, value -> config.guardrailConfig.captureCooldownSeconds = value })
        binder.forField(allowHeapDump)
            .bind({ it.guardrailConfig.allowHeapDump }, { config, value -> config.guardrailConfig.allowHeapDump = value })
        binder.forField(allowJps)
            .bind({ it.guardrailConfig.allowJps }, { config, value -> config.guardrailConfig.allowJps = value })
        binder.forField(allowJfr)
            .bind({ it.guardrailConfig.allowJfr }, { config, value -> config.guardrailConfig.allowJfr = value })
        binder.forField(apiAllowedIps)
            .bind({ it.guardrailConfig.apiAllowedIps }, { config, value -> config.guardrailConfig.apiAllowedIps = value.trim() })
    }

    companion object {
        const val ID_MCP_READ_ONLY = "guardrails-mcp-readonly"
        const val ID_MAX_RECORDING = "guardrails-max-recording"
        const val ID_CAPTURE_COOLDOWN = "guardrails-capture-cooldown"
        const val ID_ALLOW_HEAP_DUMP = "guardrails-allow-heap-dump"
        const val ID_ALLOW_JPS = "guardrails-allow-jps"
        const val ID_ALLOW_JFR = "guardrails-allow-jfr"
        const val ID_ALLOWED_IPS = "guardrails-allowed-ips"
    }
}
