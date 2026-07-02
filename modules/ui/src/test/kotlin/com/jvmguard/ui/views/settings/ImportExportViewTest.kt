package com.jvmguard.ui.views.settings

import com.jvmguard.data.user.AccessLevel
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.upload.Upload
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImportExportViewTest : JvmGuardBrowserlessTest() {

    @BeforeEach
    fun setUp() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.ADMIN)))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
    }

    @Test
    fun rendersExportAndImportControls() {
        UI.getCurrent().navigate(ImportExportView::class.java)

        find<Upload>().single()
        assertTrue(find<Button>().all().any { it.text == "Export configuration" }, "the export action is present")
    }
}
