package dev.jvmguard.ui.components

import com.vaadin.flow.component.Key
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.details.Details
import com.vaadin.flow.component.html.Pre
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import dev.jvmguard.ui.server.t

class ErrorDialog(
    title: String,
    message: String,
    stackTrace: String?,
    reloadOnClose: Boolean = false,
) : JvmGuardDialog() {

    constructor(throwable: Throwable, reloadOnClose: Boolean = false) :
            this(t("error.internal.title"), throwable.message ?: throwable.javaClass.name, throwable.stackTraceToString(), reloadOnClose)

    init {
        headerTitle = title
        width = "48rem"

        val content = VerticalLayout(Span(message)).apply {
            isPadding = false
            isSpacing = false
            style.set("gap", "0.5rem")
        }
        if (!stackTrace.isNullOrEmpty()) {
            content.add(Details(t("error.details"), Pre(stackTrace).apply { addClassName("jvmguard-stacktrace") }))
        }
        add(content)

        val ok = Button(t("common.ok")) { close() }.apply { addThemeVariants(ButtonVariant.PRIMARY) }
        ok.addClickShortcut(Key.ENTER).listenOn(this)
        footer.add(ok)

        if (reloadOnClose) {
            addOpenedChangeListener { event ->
                if (!event.isOpened) {
                    UI.getCurrent()?.page?.reload()
                }
            }
        }
    }
}
