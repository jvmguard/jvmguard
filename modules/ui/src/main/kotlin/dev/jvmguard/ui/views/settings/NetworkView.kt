package dev.jvmguard.ui.views.settings

import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/network", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class NetworkView : AbstractSettingsSectionView() {

    private val mcpEnabledInConfig = JvmGuardConfig.properties().isMcpEnabled
    private val restEnabledInConfig = JvmGuardConfig.properties().isRestApiEnabled
    private val networkEnabledInConfig = mcpEnabledInConfig || restEnabledInConfig

    private val mcpReadOnly = Checkbox("MCP read-only mode (blocks all mutating actions and diagnostic captures)").apply {
        testId = ID_MCP_READ_ONLY
        isEnabled = mcpEnabledInConfig
    }
    private val fineGrainedNote = Span(
        "Fine-grained, per-VM-group restrictions on individual captures and mutating actions are configured in the " +
            "recording settings under \"Agent guardrails\".",
    ).apply { addClassName("jvmguard-field-hint") }
    private val mcpDisabledNote = Span(
        "The MCP server is disabled in the server configuration (application.yaml), so read-only mode has no effect.",
    ).apply {
        addClassName("jvmguard-field-hint")
        isVisible = !mcpEnabledInConfig
    }

    private val apiAllowedIps = TextField("Allowed API client IP addresses").apply {
        testId = ID_ALLOWED_IPS
        setWidthFull()
        isEnabled = networkEnabledInConfig
        helperText = "Comma-separated IPv4/IPv6 addresses or CIDR ranges for the MCP server and REST API. Empty allows any address."
    }
    private val networkDisabledNote = Span(
        "Both the MCP server and the REST API are disabled in the server configuration (application.yaml), " +
            "so the IP allowlist has no effect.",
    ).apply {
        addClassName("jvmguard-field-hint")
        isVisible = !networkEnabledInConfig
    }

    init {
        add(
            settingsSection("MCP access", mcpReadOnly, fineGrainedNote, mcpDisabledNote),
            settingsSection("Allowed IP addresses", apiAllowedIps, networkDisabledNote),
        )
    }

    override fun bind(binder: Binder<GlobalConfig>) {
        binder.forField(mcpReadOnly)
            .bind({ it.guardrailConfig.mcpReadOnly }, { config, value -> config.guardrailConfig.mcpReadOnly = value })
        binder.forField(apiAllowedIps)
            .bind({ it.guardrailConfig.apiAllowedIps }, { config, value -> config.guardrailConfig.apiAllowedIps = value.trim() })
    }

    companion object {
        const val ID_MCP_READ_ONLY = "network-mcp-readonly"
        const val ID_ALLOWED_IPS = "network-allowed-ips"
    }
}
