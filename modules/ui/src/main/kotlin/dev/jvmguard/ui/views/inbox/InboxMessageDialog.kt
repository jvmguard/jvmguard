package dev.jvmguard.ui.views.inbox

import dev.jvmguard.ui.components.JvmGuardDialog
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Pre

class InboxMessageDialog(title: String, message: String) : JvmGuardDialog() {

    init {
        headerTitle = title.ifBlank { "Message" }
        width = "48rem"

        add(Pre(message).apply { addClassName("jvmguard-inbox-message") })

        val close = Button("Close") { close() }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_CLOSE
        }
        close.addClickShortcut(Key.ENTER).listenOn(this)
        footer.add(close)
    }

    companion object {
        const val ID_CLOSE = "inbox-message-close"
    }
}
