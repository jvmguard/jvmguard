package dev.jvmguard.ui.components

import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.confirmdialog.ConfirmDialog

fun confirm(header: String, text: String, confirmText: String = t("common.ok"), onConfirm: () -> Unit) {
    ConfirmDialog().apply {
        setHeader(header)
        setText(text)
        setCancelable(true)
        setConfirmText(confirmText)
        addConfirmListener { onConfirm() }
        open()
    }
}
