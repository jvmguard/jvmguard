package com.jvmguard.ui.components.recording.telemetries

import com.jvmguard.agent.config.telemetry.MBeanTelemetryConfig
import com.jvmguard.agent.config.telemetry.TelemetryUnit
import com.jvmguard.ui.components.EnumSelect
import com.jvmguard.ui.components.JvmGuardDialog
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
    private val name = TextField("Name").apply { setWidthFull(); testId = ID_NAME }
    private val unit = EnumSelect("Unit", TelemetryUnit::class.java) { it.toString() }
    private val scale = IntegerField("Scale (10^-n)").apply { width = "10rem" }
    private val groupAveraged = Checkbox("Average values from all VMs in the group")
    private val stacked = Checkbox("Stack all lines and show an area graph")

    init {
        headerTitle = if (isNew) "Add telemetry" else "Edit telemetry"
        width = "40rem"
        // Auto-size to the content
        isResizable = false

        add(VerticalLayout(name, unit, scale, groupAveraged, stacked).apply { isPadding = false; isSpacing = true })

        binder.forField(name)
            .asRequired("Enter a name.")
            .withValidator({ !nameTaken(it.trim()) }, "A telemetry with this name already exists.")
            .bind({ it.name }, { c, v -> c.name = v.trim() })
        binder.forField(unit).bind({ it.unit }, { c, v -> c.unit = v })
        binder.forField(scale).bind({ it.scale }, { c, v -> c.scale = v ?: 0 })
        binder.forField(groupAveraged).bind({ it.isGroupAveraged }, { c, v -> c.isGroupAveraged = v })
        binder.forField(stacked).bind({ it.isStacked }, { c, v -> c.isStacked = v })
        binder.readBean(config)

        confirmFooter("Save", ID_SAVE) { save() }
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
