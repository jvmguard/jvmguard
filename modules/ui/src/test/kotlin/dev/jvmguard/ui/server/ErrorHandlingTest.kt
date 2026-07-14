package dev.jvmguard.ui.server

import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.components.ErrorDialog
import com.vaadin.flow.component.details.Details
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.server.ErrorEvent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ErrorHandlingTest : JvmGuardBrowserlessTest() {

    @Test
    fun errorDialogShowsTheMessageAndStackTrace() {
        ErrorDialog(RuntimeException("kaboom")).open()

        find<ErrorDialog>().single()
        assertTrue(find<Span>().all().any { it.text == "kaboom" }, "the message is shown")
        assertTrue(find<Details>().all().isNotEmpty(), "the stack trace is in a Details disclosure")
    }

    @Test
    fun errorHandlerSurfacesUncaughtExceptionsInADialog() {
        JvmGuardErrorHandler().error(ErrorEvent(IllegalStateException("kaboom")))
        roundTrip()

        find<ErrorDialog>().single()
        assertTrue(find<Span>().all().any { it.text == "kaboom" }, "the handler surfaces the exception message")
    }
}
