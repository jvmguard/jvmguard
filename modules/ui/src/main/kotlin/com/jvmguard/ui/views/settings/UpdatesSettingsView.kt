package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.user.Roles
import com.jvmguard.ui.components.Notifications
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/updates", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class UpdatesSettingsView : AbstractSettingsSectionView() {

    private val version = Span().apply { addClassName("jvmguard-settings-version") }
    private val checkDaily = Checkbox("Check for updates once a day").apply {
        testId = ID_CHECK_DAILY
        addClassName("jvmguard-settings-spacious")
    }
    private val checkNow = Button("Check for updates now") { checkNow() }

    init {
        add(settingsSection("Updates", version, checkDaily, checkNow))
        loadVersion()
    }

    override fun bind(binder: Binder<GlobalConfig>) {
        binder.forField(checkDaily)
            .bind({ it.checkForUpdates }, { config, value -> config.checkForUpdates = value })
    }

    private fun loadVersion() {
        Sessions.current()?.serverConnection?.installationInfo?.let {
            version.text = "Installed version ${it.version} (build ${it.build})"
        }
    }

    private fun checkNow() {
        val result = Sessions.current()?.serverConnection?.checkForUpdates()
        val update = result?.updateVersion
        if (!update.isNullOrEmpty() && update != result.installedVersion) {
            Notifications.show("Update available: $update (installed ${result.installedVersion}).")
        } else {
            Notifications.show("jvmguard is up to date.")
        }
    }

    companion object {
        const val ID_CHECK_DAILY = "settings-check-updates"
    }
}
