package dev.jvmguard.ui.components.recording.telemetries

import dev.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import dev.jvmguard.agent.config.telemetry.TelemetryUnit
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class TelemetryConfigDialog(
    private val config: MBeanTelemetryConfig,
    isNew: Boolean,
    private val nameTaken: (String) -> Boolean,
    private val onSave: (MBeanTelemetryConfig) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(MBeanTelemetryConfig::class.java)
    private val name = TextField(t("recording.name")).apply { setWidthFull(); testId = ID_NAME }
    private val unit = EnumSelect(t("telemetry.config.unit"), TelemetryUnit::class.java)
    private val scale = IntegerField(t("telemetry.config.scale")).apply { width = "10rem" }
    private val groupAveraged = Checkbox(t("telemetry.config.groupAveraged"))
    private val stacked = Checkbox(t("telemetry.config.stacked"))

    init {
        headerTitle = t(if (isNew) "telemetry.dialog.add" else "telemetry.dialog.edit")
        width = "40rem"
        // Auto-size to the content
        isResizable = false

        add(VerticalLayout(name, unit, scale, groupAveraged, stacked).apply { isPadding = false; isSpacing = true })

        binder.forField(name)
            .asRequired(t("recording.validation.nameRequired"))
            .withValidator({ !nameTaken(it.trim()) }, t("telemetry.config.nameTaken"))
            .bind({ it.name }, { c, v -> c.name = v.trim() })
        binder.forField(unit).bind({ it.unit }, { c, v -> c.unit = v })
        binder.forField(scale).bind({ it.scale }, { c, v -> c.scale = v ?: 0 })
        binder.forField(groupAveraged).bind({ it.isGroupAveraged }, { c, v -> c.isGroupAveraged = v })
        binder.forField(stacked).bind({ it.isStacked }, { c, v -> c.isStacked = v })
        binder.readBean(config)

        confirmFooter(t("common.save"), ID_SAVE) { save() }
    }

    private fun save() {
        if (!binder.writeBeanIfValid(config)) {
            return
        }
        onSave(config)
        close()
    }

    companion object {
        const val ID_NAME = "telemetry-name"
        const val ID_SAVE = "telemetry-save"
    }
}
