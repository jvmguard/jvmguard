package com.jvmguard.ui.views.account

import com.jvmguard.ui.JvmGuardBrowserlessTest
import com.jvmguard.ui.server.MockConnections
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.UserSession
import com.jvmguard.connector.server.mock.MockServerConnectionImpl
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccountViewTest : JvmGuardBrowserlessTest() {

    private lateinit var connection: MockServerConnectionImpl

    @BeforeEach
    fun setUp() {
        connection = MockConnections.create()
        Sessions.setCurrent(UserSession(connection))
    }

    @AfterEach
    fun tearDown() {
        Sessions.setCurrent(null)
        Sessions.clearAccountDraft()
    }

    private fun field(label: String): TextField = find<TextField>().all().first { it.label == label }
    private fun password(label: String): PasswordField = find<PasswordField>().all().first { it.label == label }
    private fun button(text: String): Button = find<Button>().all().first { it.text == text }
    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }
    private fun cancel(): Button = find<Button>().all().first { it.text == "Cancel" }

    @Test
    fun cancellingDiscardsProfileChanges() {
        UI.getCurrent().navigate(AccountProfileView::class.java)
        use(field("Full name")).setValue("Changed")
        use(cancel()).click()

        assertEquals("Administrator", connection.user.fullName, "Cancel must not persist the edit")
    }

    @Test
    fun loadsTheCurrentProfile() {
        UI.getCurrent().navigate(AccountProfileView::class.java)
        assertEquals("Administrator", field("Full name").value)
    }

    @Test
    fun savingTheProfileWritesItBack() {
        UI.getCurrent().navigate(AccountProfileView::class.java)
        use(field("Full name")).setValue("New Name")
        use(shellSave()).click()

        assertEquals("New Name", connection.user.fullName)
    }

    @Test
    fun mismatchedPasswordsBlockSave() {
        UI.getCurrent().navigate(AccountProfileView::class.java)
        use(password("New password")).setValue("abcdef")
        use(password("Confirm new password")).setValue("different")
        use(shellSave()).click()

        assertInstanceOf(AccountProfileView::class.java, currentView)
        assertTrue(password("Confirm new password").isInvalid)
    }

    @Test
    fun generatingAnApiKeyStoresAHashOnSave() {
        UI.getCurrent().navigate(AccountApiKeyView::class.java)
        use(button("Generate new API key")).click()
        assertTrue(!field("API key").value.isNullOrEmpty(), "a key is shown once")

        use(shellSave()).click()
        assertTrue(find<AccountApiKeyView>().all().isEmpty()) // navigated away after save
        assertTrue(connection.user.apiKeyHash.isNotEmpty(), "the hashed key is persisted")
    }

    private fun mcpSnippet(): String =
        find<Span>().all().first { "jvmguard-mcp-snippet" in it.classNames }.text

    @Test
    fun mcpSnippetShowsPlaceholderUntilAKeyIsGenerated() {
        UI.getCurrent().navigate(AccountApiKeyView::class.java)
        val before = mcpSnippet()
        assertTrue("claude mcp add" in before, "default snippet is the Claude Code command: $before")
        assertTrue("<YOUR_API_KEY>" in before, "snippet shows a placeholder before a key exists: $before")

        use(button("Generate new API key")).click()
        val key = field("API key").value
        assertTrue(!key.isNullOrEmpty(), "a key is shown once")

        val after = mcpSnippet()
        assertTrue(key in after, "snippet embeds the freshly generated key: $after")
        assertFalse("<YOUR_API_KEY>" in after, "placeholder is replaced once a key is visible: $after")
    }
}
