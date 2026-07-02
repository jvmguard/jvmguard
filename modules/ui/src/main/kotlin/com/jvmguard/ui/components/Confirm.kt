package com.jvmguard.ui.components

import com.vaadin.flow.component.confirmdialog.ConfirmDialog

fun confirm(header: String, text: String, confirmText: String = "OK", onConfirm: () -> Unit) {
    ConfirmDialog().apply {
        setHeader(header)
        setText(text)
        setCancelable(true)
        setConfirmText(confirmText)
        addConfirmListener { onConfirm() }
        open()
    }
}
