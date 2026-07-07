package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.DefaultTheme
import com.jvmguard.data.config.FrequencyUnit
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.user.Roles
import com.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import com.jvmguard.ui.components.EnumSelect
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/display", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class DisplaySettingsView : AbstractSettingsSectionView() {

    private val titleEnabled = Checkbox("Use a custom window title").apply {
        testId = ID_TITLE_ENABLED
        addValueChangeListener { titleText.isEnabled = value }
    }
    private val titleText = TextField("Window title").apply {
        setWidthFull()
        testId = ID_TITLE_TEXT
    }
    private val defaultTheme = EnumSelect("Default theme", DefaultTheme::class.java) { it.toString() }
    private val frequencyUnit = EnumSelect("Frequency unit for telemetries", FrequencyUnit::class.java) { it.toString() }
    private val hiddenTelemetries = MultiSelectComboBox<String>("Hidden Declared telemetries").apply {
        setWidthFull()
        testId = ID_HIDDEN_TELEMETRIES
        addValueChangeListener { event ->
            if (event.isFromClient) {
                val draft = Sessions.settingsDraft()
                draft.hiddenTelemetries = event.value
                draft.markDirty()
            }
        }
    }

    init {
        val frequencyHint = Span("Many small telemetries display frequency values; choose the unit that matches the average transaction volume.")
            .apply { addClassName("jvmguard-field-hint") }
        val telemetryHint = Span("Telemetries added with the @Telemetry annotation can be hidden when no longer needed.")
            .apply { addClassName("jvmguard-field-hint") }
        add(settingsSection("Display", titleEnabled, titleText, defaultTheme, frequencyUnit, frequencyHint, hiddenTelemetries, telemetryHint))
    }

    @Suppress("DuplicatedCode")
    override fun bind(binder: Binder<GlobalConfig>) {
        binder.forField(titleEnabled)
            .bind({ it.windowTitle.isChecked }, { config, value -> config.windowTitle.isChecked = value })
        binder.forField(titleText)
            .bind({ it.windowTitle.value.orEmpty() }, { config, value -> config.windowTitle.value = value })
        binder.forField(defaultTheme)
            .bind({ it.defaultTheme }, { config, value -> config.defaultTheme = value })
        binder.forField(frequencyUnit)
            .bind({ it.frequencyUnit }, { config, value -> config.frequencyUnit = value })
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        titleText.isEnabled = titleEnabled.value
        loadTelemetries()
    }

    private fun loadTelemetries() {
        val draft = Sessions.settingsDraft()
        if (draft.allTelemetryNodes.isEmpty()) {
            val connection = Sessions.current()?.serverConnection ?: return
            val declaredNodes = connection.customTelemetryInfo.customTelemetryNodeIdentifiers
                .filter { it.type == CustomTelemetryNodeIdentifier.Type.DECLARED }
                .map { it.name }
            val currentlyHidden = connection.hiddenDeclaredTelemetryNodes.toSet()
            draft.allTelemetryNodes = (declaredNodes + currentlyHidden).distinct().sorted()
            if (draft.hiddenTelemetries == null) {
                draft.hiddenTelemetries = currentlyHidden
            }
        }
        hiddenTelemetries.setItems(draft.allTelemetryNodes)
        hiddenTelemetries.value = draft.hiddenTelemetries.orEmpty()
    }

    companion object {
        const val ID_TITLE_ENABLED = "settings-title-enabled"
        const val ID_TITLE_TEXT = "settings-title-text"
        const val ID_HIDDEN_TELEMETRIES = "settings-hidden-telemetries"
    }
}
