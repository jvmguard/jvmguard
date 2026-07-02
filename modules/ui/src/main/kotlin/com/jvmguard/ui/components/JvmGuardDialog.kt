package com.jvmguard.ui.components

import com.vaadin.flow.component.*
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.server.Command

open class JvmGuardDialog : Dialog() {

    init {
        isResizable = true
        isDraggable = true
        isKeepInViewport = true
        Shortcuts.addShortcutListener(this, Command { confirm() }, Key.ENTER, KeyModifier.CONTROL).listenOn(this)
        Shortcuts.addShortcutListener(this, Command { confirm() }, Key.ENTER, KeyModifier.META).listenOn(this)
    }

    protected fun confirmFooter(confirmText: String, confirmTestId: String? = null, onConfirm: () -> Unit): Button {
        val confirm = Button(confirmText) { onConfirm() }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            confirmTestId?.let { testId = it }
        }
        footer.add(Button("Cancel") { close() }, confirm)
        return confirm
    }

    private fun confirm() {
        val primary = footerButtons().lastOrNull { "primary" in it.themeNames } ?: return
        if (primary.isEnabled) {
            ComponentUtil.fireEvent(primary, ClickEvent(primary))
        }
    }

    private fun footerButtons(): List<Button> =
        footer.element.children.toList().mapNotNull { it.component.orElse(null) as? Button }
}
