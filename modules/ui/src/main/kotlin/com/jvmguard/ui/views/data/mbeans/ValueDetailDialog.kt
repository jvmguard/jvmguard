package com.jvmguard.ui.views.data.mbeans

import com.jvmguard.ui.components.JvmGuardDialog
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.textfield.TextArea

class ValueDetailDialog(text: String) : JvmGuardDialog() {

    init {
        headerTitle = "Detail"
        width = "46rem"
        height = "32rem"

        val area = TextArea("Entire text of the selected value").apply {
            value = text
            isReadOnly = true
            setSizeFull()
            testId = ID_TEXT
        }
        add(area)

        val close = Button("Close") { close() }.apply { addThemeVariants(ButtonVariant.PRIMARY) }
        close.addClickShortcut(Key.ENTER).listenOn(this)
        footer.add(close)
    }

    companion object {
        const val ID_TEXT = "mbean-value-detail-text"
    }
}
