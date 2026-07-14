package dev.jvmguard.ui.components

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.server.streams.DownloadHandler
import com.vaadin.flow.server.streams.DownloadResponse
import java.io.ByteArrayInputStream

class ExportAnchor(testId: String) : Anchor() {

    init {
        add(Button("Export", VaadinIcon.DOWNLOAD.create()).apply { addThemeVariants(ButtonVariant.TERTIARY) })
        element.setAttribute("download", true)
        isVisible = false
        this.testId = testId
        style.set("margin-inline-start", "auto")
    }

    fun setJsonContent(bytes: ByteArray, fileName: String) {
        setHref(DownloadHandler.fromInputStream {
            DownloadResponse(ByteArrayInputStream(bytes), fileName, "application/json", bytes.size.toLong())
        })
        isVisible = true
    }
}
