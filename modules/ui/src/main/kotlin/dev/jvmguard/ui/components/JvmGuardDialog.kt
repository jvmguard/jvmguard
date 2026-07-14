package dev.jvmguard.ui.components

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
        isCloseOnOutsideClick = false
        Shortcuts.addShortcutListener(this, Command { confirm() }, Key.ENTER, KeyModifier.CONTROL).listenOn(this)
        Shortcuts.addShortcutListener(this, Command { confirm() }, Key.ENTER, KeyModifier.META).listenOn(this)
        // A dialog that auto-sizes to its content (no explicit height) must give [part=content] room for
        // its last control (see styles.css), otherwise a trailing control that overflows its box clips and
        // draws a phantom scrollbar. A dialog with a fixed height — or one the user has resized — instead
        // fills the fixed box and keeps Vaadin's default content sizing. The overlay only exists once
        // opened, and a subclass sets its height in its own init (after this one), so decide on open.
        addOpenedChangeListener { if (it.isOpened) updateFitContentTheme() }
        addResizeListener { element.themeList.remove(THEME_FIT_CONTENT) }
    }

    /** Mark the dialog as content-sized unless it has an explicit height to fill. */
    private fun updateFitContentTheme() {
        if (height.isNullOrEmpty()) {
            element.themeList.add(THEME_FIT_CONTENT)
        } else {
            element.themeList.remove(THEME_FIT_CONTENT)
        }
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

    companion object {
        /** Overlay theme (see styles.css) that makes `[part=content]` size to its content, not fill. */
        const val THEME_FIT_CONTENT = "jvmguard-fit-content"
    }
}
