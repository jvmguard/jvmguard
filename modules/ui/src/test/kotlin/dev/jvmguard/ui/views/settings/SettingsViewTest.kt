package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import dev.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.IntegerField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SettingsViewTest : JvmGuardBrowserlessTest() {

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.clearSettingsDraft()
    }

    @Test
    fun adminOpensSettingsAtTheFirstSection() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.ADMIN)))
        UI.getCurrent().navigate(SettingsView::class.java)

        assertInstanceOf(UsersView::class.java, currentView)
    }

    @Test
    fun nonAdminIsForwardedToVms() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.VIEWER)))
        UI.getCurrent().navigate(SettingsView::class.java)

        assertFalse(UsersView::class.java.isInstance(currentView))
        assertInstanceOf(VmsView::class.java, currentView)
    }

    @Test
    fun saveIsDisabledUntilAChange() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.ADMIN)))
        UI.getCurrent().navigate(DataSettingsView::class.java)

        val save = find<Button>().all().first { it.text == "Save" }
        assertFalse(save.isEnabled, "Save is disabled with no changes")

        use(find<IntegerField>().all().first()).setValue(9999)
        assertTrue(save.isEnabled, "a change enables Save")
    }

    @Test
    fun editingDataRetentionPersistsOnSave() {
        val connection = MockConnections.create(AccessLevel.ADMIN)
        Sessions.setCurrent(UserSession(connection))
        UI.getCurrent().navigate(DataSettingsView::class.java)

        use(find<IntegerField>().all().first { it.testId == DataSettingsView.ID_TRANSACTION_CAP }).setValue(12345)
        use(find<Button>().all().first { it.text == "Save" }).click()

        assertEquals(12345, connection.getGlobalConfig(false).transactionCap)
    }
}
