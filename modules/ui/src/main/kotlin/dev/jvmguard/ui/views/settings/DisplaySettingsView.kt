package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.DefaultTheme
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.user.Roles
import dev.jvmguard.data.vmdata.CustomTelemetryNodeIdentifier
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/display", layout = MainLayout::class)
class DisplaySettingsView : AbstractSettingsSectionView() {

    private val titleEnabled = Checkbox(t("settings.display.customTitle")).apply {
        testId = ID_TITLE_ENABLED
        addValueChangeListener { titleText.isEnabled = value }
    }
    private val titleText = TextField(t("settings.display.windowTitle")).apply {
        setWidthFull()
        testId = ID_TITLE_TEXT
    }
    private val defaultTheme = EnumSelect(t("settings.display.defaultTheme"), DefaultTheme::class.java)
    private val frequencyUnit = EnumSelect(t("settings.display.frequencyUnit"), FrequencyUnit::class.java)
    private val hiddenTelemetries = MultiSelectComboBox<String>(t("settings.display.hiddenTelemetries")).apply {
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
        val frequencyHint = Span(t("settings.display.frequencyHint"))
            .apply { addClassName("jvmguard-field-hint") }
        val telemetryHint = Span(t("settings.display.telemetryHint"))
            .apply { addClassName("jvmguard-field-hint") }
        add(settingsSection(t("nav.settings.display"), titleEnabled, titleText, defaultTheme, frequencyUnit, frequencyHint, hiddenTelemetries, telemetryHint))
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
