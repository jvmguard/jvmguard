package com.jvmguard.ui.e2e.config

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import com.jvmguard.ui.e2e.config.LdapE2ETest.Companion.LDAP_PORT
import com.jvmguard.ui.shell.MainLayout
import com.jvmguard.ui.views.settings.LdapMappingDialog
import com.jvmguard.ui.views.settings.LdapView
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import com.unboundid.ldif.LDIFReader
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

/**
 * An embedded UnboundID in-memory directory runs in this test JVM on [LDAP_PORT]. The jvmguard e2e
 * server is a separate process, so it is pointed at ldap://localhost:<port> over the network via the UI.
 */
class LdapE2ETest : ConfigE2ETest() {

    private lateinit var ldapServer: InMemoryDirectoryServer

    @BeforeAll
    fun startLdap() {
        val config = InMemoryDirectoryServerConfig("dc=example,dc=com").apply {
            // Test fixture: skip schema validation so the sample LDIF loads as-is.
            schema = null
            // The admin bind DN lives outside the naming context
            addAdditionalBindCredentials("uid=admin,ou=system", ADMIN_PASSWORD)
            setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", LDAP_PORT))
        }
        ldapServer = InMemoryDirectoryServer(config)
        LDIFReader(ByteArrayInputStream(TEST_LDIF.toByteArray())).use { reader ->
            ldapServer.importFromLDIF(true, reader)
        }
        ldapServer.startListening()
    }

    @AfterAll
    fun stopLdap() {
        if (::ldapServer.isInitialized) {
            runCatching { ldapServer.shutDown(true) }
        }
    }

    @Test
    fun ldapUserLogsInViaUserMapping() = onPage {
        freshServer()
        loginRealAndWaitForApp()

        openGeneralSettings("settings/ldap")
        getByTestId(LdapView.ID_URL).locator("input").fill("ldap://localhost:$LDAP_PORT")
        getByTestId(LdapView.ID_AUTHENTICATE).locator("input").check()
        getByTestId(LdapView.ID_USER_NAME).locator("input").fill("uid=admin,ou=system")
        getByTestId(LdapView.ID_PASSWORD).locator("input").fill(ADMIN_PASSWORD)

        getByTestId(LdapView.ID_ADD_MAPPING).click()
        getByTestId(LdapMappingDialog.ID_SEARCH_BASE).locator("input").fill("ou=Users,dc=example,dc=com")
        getByTestId(LdapMappingDialog.ID_USER_FILTER).locator("input").fill("(cn=@USER@)")
        getByTestId(LdapMappingDialog.ID_ACCESS_LEVEL).click()
        getByRole(AriaRole.OPTION, Page.GetByRoleOptions().setName("Admin").setExact(true)).click()
        getByTestId(LdapMappingDialog.ID_SAVE).click()
        applySettings()

        // "Jane Doe" exists only in LDAP, so a successful login proves the mapping resolved against it.
        logout()
        loginRealAndWaitForApp("Jane Doe", "test91777253")
        getByTestId(MainLayout.ID_SETTINGS).click()
        assertThat(getByTestId(MainLayout.ID_GENERAL_SETTINGS)).isVisible()
    }

    private companion object {
        const val LDAP_PORT = 10389
        const val ADMIN_PASSWORD = "secret92763"
        val TEST_LDIF = """
            dn: dc=example,dc=com
            objectClass: domain
            objectClass: top
            dc: example

            dn: ou=Users,dc=example,dc=com
            objectClass: organizationalUnit
            objectClass: top
            ou: Users

            dn: ou=Groups,dc=example,dc=com
            objectClass: organizationalUnit
            objectClass: top
            ou: Groups

            dn: cn=Jane Doe,ou=Users,dc=example,dc=com
            objectClass: inetOrgPerson
            objectClass: organizationalPerson
            objectClass: person
            objectClass: top
            cn: Jane Doe
            sn: Doe
            givenName: Jane
            mail: jane@example.com
            uid: jdoe
            userPassword: test91777253

            dn: cn=John Wick,ou=Users,dc=example,dc=com
            objectClass: inetOrgPerson
            objectClass: organizationalPerson
            objectClass: person
            objectClass: top
            cn: John Wick
            sn: Wick
            givenName: John
            mail: john@example.com
            uid: jwick
            userPassword: test362244425
        """.trimIndent() + "\n"
    }
}
