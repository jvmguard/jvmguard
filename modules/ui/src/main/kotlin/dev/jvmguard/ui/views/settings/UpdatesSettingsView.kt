package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.user.Roles
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/updates", layout = MainLayout::class)
class UpdatesSettingsView : AbstractSettingsSectionView() {

    private val version = Span().apply { addClassName("jvmguard-settings-version") }
    private val checkDaily = Checkbox(t("settings.updates.checkDaily")).apply {
        testId = ID_CHECK_DAILY
        addClassName("jvmguard-settings-spacious")
    }
    private val checkNow = Button(t("settings.updates.checkNow")) { checkNow() }

    init {
        add(settingsSection(t("nav.settings.updates"), version, checkDaily, checkNow))
        loadVersion()
    }

    override fun bind(binder: Binder<GlobalConfig>) {
        binder.forField(checkDaily)
            .bind({ it.checkForUpdates }, { config, value -> config.checkForUpdates = value })
    }

    private fun loadVersion() {
        Sessions.current()?.serverConnection?.installationInfo?.let {
            version.text = t("settings.updates.installed", it.version, it.build)
        }
    }

    private fun checkNow() {
        val result = Sessions.current()?.serverConnection?.checkForUpdates()
        val update = result?.updateVersion
        if (!update.isNullOrEmpty() && update != result.installedVersion) {
            Notifications.show(t("settings.updates.available", update, result.installedVersion))
        } else {
            Notifications.show(t("settings.updates.upToDate"))
        }
    }

    companion object {
        const val ID_CHECK_DAILY = "settings-check-updates"
    }
}
