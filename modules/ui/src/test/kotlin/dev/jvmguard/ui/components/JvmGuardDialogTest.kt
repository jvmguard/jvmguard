package dev.jvmguard.ui.components

import dev.jvmguard.ui.JvmGuardBrowserlessTest
import com.vaadin.flow.component.html.Span
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JvmGuardDialogTest : JvmGuardBrowserlessTest() {

    private class FixedHeightDialog : JvmGuardDialog() {
        init {
            height = "30rem"
            add(Span("content"))
        }
    }

    @Test
    fun autoSizedDialogIsMarkedFitContent() {
        // A height-less dialog must let [part=content] size to its content (see styles.css), or a trailing
        // control (e.g. a checkbox whose native input overflows the host) is clipped by a phantom scrollbar.
        val dialog = JvmGuardDialog().apply { add(Span("content")) }
        dialog.open()
        assertTrue(
            JvmGuardDialog.THEME_FIT_CONTENT in dialog.element.themeList,
            "an auto-sized dialog must carry the fit-content overlay theme",
        )
    }

    @Test
    fun fixedHeightDialogFillsInsteadOfFittingContent() {
        val dialog = FixedHeightDialog()
        dialog.open()
        assertFalse(
            JvmGuardDialog.THEME_FIT_CONTENT in dialog.element.themeList,
            "a fixed-height dialog must fill its box, not shrink to content",
        )
    }

    @Test
    fun dialogsDoNotCloseOnOutsideClick() {
        assertFalse(JvmGuardDialog().isCloseOnOutsideClick, "a stray outside click must not discard edits")
    }
}
