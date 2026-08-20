package dev.jvmguard.ui.views.data.mbeans

import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.textfield.TextArea

class ValueDetailDialog(text: String) : JvmGuardDialog() {

    init {
        headerTitle = t("mbeans.value.detail.title")
        width = "46rem"
        height = "32rem"

        val area = TextArea(t("mbeans.value.detail.label")).apply {
            value = text
            isReadOnly = true
            setSizeFull()
            testId = ID_TEXT
        }
        add(area)

        val close = Button(t("common.close")) { close() }.apply { addThemeVariants(ButtonVariant.PRIMARY) }
        close.addClickShortcut(Key.ENTER).listenOn(this)
        footer.add(close)
    }

    companion object {
        const val ID_TEXT = "mbean-value-detail-text"
    }
}
