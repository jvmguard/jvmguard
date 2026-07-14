package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.DefaultTheme
import dev.jvmguard.data.config.SmtpConfig.Encryption
import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import dev.jvmguard.connector.server.mock.MockServerConnectionImpl
import com.vaadin.flow.component.AbstractField
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GlobalSettingsSectionsTest : JvmGuardBrowserlessTest() {

    private lateinit var connection: MockServerConnectionImpl

    @BeforeEach
    fun setUp() {
        connection = MockConnections.create(AccessLevel.ADMIN)
        Sessions.setCurrent(UserSession(connection))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.clearSettingsDraft()
    }

    private fun textField(label: String): TextField = find<TextField>().all().first { it.label == label }
    private fun checkbox(label: String): Checkbox = find<Checkbox>().all().first { it.label == label }
    private fun save(): Button = find<Button>().all().first { it.text == "Save" }

    @Test
    fun editingSmtpPersistsOnSave() {
        UI.getCurrent().navigate(SmtpSettingsView::class.java)

        use(textField("SMTP host")).setValue("mail.example.com")
        @Suppress("UNCHECKED_CAST")
        (find<Select<*>>().all().first { it.label == "Encryption" } as Select<Encryption>).value = Encryption.STARTTLS
        use(save()).click()

        val smtp = connection.getGlobalConfig(false).smtpConfig
        assertEquals("mail.example.com", smtp.host)
        assertEquals(Encryption.STARTTLS, smtp.encryption)
    }

    @Test
    fun authenticationFieldsFollowTheCheckbox() {
        UI.getCurrent().navigate(SmtpSettingsView::class.java)

        val userName = textField("Username")
        assertFalse(userName.isEnabled, "auth fields are disabled until authentication is on")

        checkbox("Authenticate with the SMTP server").value = true
        assertTrue(userName.isEnabled, "enabling authentication enables the credentials")
    }

    @Test
    fun editingDisplayPersistsOnSave() {
        UI.getCurrent().navigate(DisplaySettingsView::class.java)

        @Suppress("UNCHECKED_CAST")
        (find<Select<*>>().all().first { it.label == "Default theme" } as Select<DefaultTheme>).value = DefaultTheme.DARK
        use(save()).click()

        assertEquals(DefaultTheme.DARK, connection.getGlobalConfig(false).defaultTheme)
    }

    @Test
    fun hiddenTelemetryComboOffersTheDeclaredNodes() {
        UI.getCurrent().navigate(DisplaySettingsView::class.java)

        @Suppress("UNCHECKED_CAST")
        val combo = find<MultiSelectComboBox<*>>().all().first { it.label == "Hidden Declared telemetries" } as MultiSelectComboBox<String>
        // MultiSelectComboBox selectItem sets the value fromClient=false, so fire the client value change to trip the isFromClient-gated listener.
        val previous = combo.value
        combo.value = setOf("Request queue length")
        ComponentUtil.fireEvent(combo, AbstractField.ComponentValueChangeEvent(combo, combo, previous, true))

        assertTrue(save().isEnabled, "selecting a telemetry to hide enables Save")
    }
}
