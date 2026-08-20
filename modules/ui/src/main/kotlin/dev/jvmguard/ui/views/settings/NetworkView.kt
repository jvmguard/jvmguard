package dev.jvmguard.ui.views.settings

import dev.jvmguard.common.JvmGuardConfig
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/network", layout = MainLayout::class)
class NetworkView : AbstractSettingsSectionView() {

    private val mcpEnabledInConfig = JvmGuardConfig.properties().isMcpEnabled
    private val restEnabledInConfig = JvmGuardConfig.properties().isRestApiEnabled
    private val networkEnabledInConfig = mcpEnabledInConfig || restEnabledInConfig

    private val mcpReadOnly = Checkbox(t("settings.network.mcpReadOnly")).apply {
        testId = ID_MCP_READ_ONLY
        isEnabled = mcpEnabledInConfig
    }
    private val fineGrainedNote = Span(t("settings.network.fineGrainedNote")).apply { addClassName("jvmguard-field-hint") }
    private val mcpDisabledNote = Span(t("settings.network.mcpDisabledNote")).apply {
        addClassName("jvmguard-field-hint")
        isVisible = !mcpEnabledInConfig
    }

    private val apiAllowedIps = TextField(t("settings.network.allowedIps")).apply {
        testId = ID_ALLOWED_IPS
        setWidthFull()
        isEnabled = networkEnabledInConfig
        helperText = t("settings.network.allowedIps.helper")
    }
    private val networkDisabledNote = Span(t("settings.network.networkDisabledNote")).apply {
        addClassName("jvmguard-field-hint")
        isVisible = !networkEnabledInConfig
    }

    init {
        add(
            settingsSection(t("settings.network.mcpSection"), mcpReadOnly, fineGrainedNote, mcpDisabledNote),
            settingsSection(t("settings.network.allowedIpsSection"), apiAllowedIps, networkDisabledNote),
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
