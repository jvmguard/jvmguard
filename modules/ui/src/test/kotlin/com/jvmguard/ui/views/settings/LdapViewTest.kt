package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.LdapUserMapping
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.connector.server.mock.MockServerConnectionImpl
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LdapViewTest : JvmGuardBrowserlessTest() {

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
    private fun button(text: String): Button = find<Button>().all().first { it.text == text }

    // The shell Save carries its own class so it can be told apart from a dialog's "Save".
    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }
    private fun dialogSave(): Button = find<Button>().all().first { it.text == "Save" && "jvmguard-settings-save" !in it.classNames }

    @Test
    fun editingTheUrlPersistsOnSave() {
        UI.getCurrent().navigate(LdapView::class.java)

        use(textField("LDAP URL")).setValue("ldaps://directory.example.com:636")
        use(shellSave()).click()

        assertTrue(connection.getGlobalConfig(false).ldapConfig.url == "ldaps://directory.example.com:636")
    }

    @Test
    fun invalidUrlBlocksTheSave() {
        UI.getCurrent().navigate(LdapView::class.java)
        use(textField("LDAP URL")).setValue("ldaps://good.example.com:636")
        use(shellSave()).click()
        assertTrue(connection.getGlobalConfig(false).ldapConfig.url == "ldaps://good.example.com:636")

        UI.getCurrent().navigate(LdapView::class.java)
        use(textField("LDAP URL")).setValue("http://wrong-scheme")
        use(shellSave()).click()

        assertTrue(connection.getGlobalConfig(false).ldapConfig.url == "ldaps://good.example.com:636")
        assertInstanceOf(LdapView::class.java, currentView)
    }

    @Test
    fun addingAMappingPersistsOnSave() {
        UI.getCurrent().navigate(LdapView::class.java)
        use(button("Add mapping")).click()

        use(textField("Search base")).setValue("ou=people,dc=example,dc=com")
        use(textField("User filter")).setValue("(uid=${LdapUserMapping.TOKEN_USER})")
        use(dialogSave()).click()

        use(shellSave()).click()

        val mappings = connection.getGlobalConfig(false).ldapConfig.userMappings
        assertFalse(mappings.isEmpty(), "the mapping was committed to the LDAP config")
        assertTrue(mappings.any { it.searchBase == "ou=people,dc=example,dc=com" })
    }
}
