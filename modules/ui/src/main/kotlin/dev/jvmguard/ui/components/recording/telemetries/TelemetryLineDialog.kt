package dev.jvmguard.ui.components.recording.telemetries

import dev.jvmguard.agent.config.telemetry.MBeanLineConfig
import dev.jvmguard.ui.components.Notifications
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder

class TelemetryLineDialog(
    private val line: MBeanLineConfig,
    isNew: Boolean,
    private val nameTaken: (String) -> Boolean,
    private val onSave: (MBeanLineConfig) -> Unit,
) : JvmGuardDialog() {

    private val binder = Binder(MBeanLineConfig::class.java)
    private val lineName = TextField(t("telemetry.line.caption")).apply { setWidthFull(); testId = ID_NAME }
    private val beanName = TextField(t("telemetry.line.beanName")).apply { setWidthFull(); isReadOnly = true; testId = ID_BEAN }
    private val attributePath = TextField(t("telemetry.line.path")).apply { setWidthFull(); isReadOnly = true; testId = ID_PATH }
    private val selectButton = Button(t("common.select"), VaadinIcon.SEARCH.create()) { openPicker() }.apply { testId = ID_SELECT }

    init {
        headerTitle = t(if (isNew) "telemetry.line.dialog.add" else "telemetry.line.dialog.edit")
        width = "44rem"
        isResizable = false

        beanName.value = line.beanName
        attributePath.value = line.attributePath

        val beanRow = HorizontalLayout(beanName, selectButton).apply {
            setWidthFull()
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            isPadding = false
            setFlexGrow(1.0, beanName)
        }
        add(VerticalLayout(lineName, beanRow, attributePath).apply { isPadding = false; isSpacing = true })

        binder.forField(lineName)
            .asRequired(t("telemetry.line.caption.required"))
            .withValidator({ !nameTaken(it.trim()) }, t("telemetry.line.caption.taken"))
            .bind({ it.lineName }, { l, v -> l.lineName = v.trim() })
        binder.readBean(line)

        confirmFooter(t("common.save"), ID_SAVE) { save() }
    }

    private fun openPicker() {
        NumericMBeanValueDialog { objectName, path, suggested ->
            beanName.value = objectName
            attributePath.value = path
            if (lineName.value.isBlank()) {
                lineName.value = suggested
            }
        }.open()
    }

    private fun save() {
        if (beanName.value.isBlank() || attributePath.value.isBlank()) {
            Notifications.show(t("telemetry.line.selectValueFirst"))
            return
        }
        if (!binder.writeBeanIfValid(line)) {
            return
        }
        line.beanName = beanName.value
        line.attributePath = attributePath.value
        onSave(line)
        close()
    }

    companion object {
        const val ID_NAME = "telemetry-line-name"
        const val ID_BEAN = "telemetry-line-bean"
        const val ID_PATH = "telemetry-line-path"
        const val ID_SELECT = "telemetry-line-select"
        const val ID_SAVE = "telemetry-line-save"
    }
}
