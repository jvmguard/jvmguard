package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.user.AccessLevel
import dev.jvmguard.data.user.UserType
import dev.jvmguard.ui.JvmGuardBrowserlessTest
import dev.jvmguard.ui.server.MockConnections
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.UserSession
import dev.jvmguard.connector.server.mock.MockServerConnectionImpl
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UsersViewTest : JvmGuardBrowserlessTest() {

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
    private fun password(label: String): PasswordField = find<PasswordField>().all().first { it.label == label }
    private fun button(text: String): Button = find<Button>().all().first { it.text == text }

    private fun shellSave(): Button = find<Button>().all().first { "jvmguard-settings-save" in it.classNames }
    private fun dialogSave(): Button = find<Button>().all().first { it.text == "Save" && "jvmguard-settings-save" !in it.classNames }

    @Test
    fun addingALocalUserStagesAndPersistsOnSave() {
        UI.getCurrent().navigate(UsersView::class.java)
        use(button("Add user")).click()

        use(textField("Login name")).setValue("newbie")
        use(password("Password")).setValue("secret")
        use(password("Confirm password")).setValue("secret")
        use(dialogSave()).click()

        assertTrue(shellSave().isEnabled, "staging a user enables Save")
        assertTrue(connection.users.none { it.loginName == "newbie" }, "not committed before Save")

        use(shellSave()).click()
        assertTrue(connection.users.any { it.loginName == "newbie" }, "the new user is committed on Save")
    }

    @Test
    fun addingAnSsoUserStagesTheEmailAsLoginName() {
        UI.getCurrent().navigate(UsersView::class.java)
        use(button("Add user")).click()

        @Suppress("UNCHECKED_CAST")
        (find<Select<*>>().all().first { it.label == "User type" } as Select<UserType>).value = UserType.OIDC
        find<EmailField>().all().first { it.label == "SSO email" }.value = "sso@example.com"
        use(dialogSave()).click()

        use(shellSave()).click()
        assertTrue(connection.users.any { it.loginName == "sso@example.com" }, "the SSO email is saved as the login name")
    }

    @Test
    fun nonAdminIsForwardedAwayFromAGridPage() {
        Sessions.setCurrent(UserSession(MockConnections.create(AccessLevel.VIEWER)))
        UI.getCurrent().navigate(UsersView::class.java)

        assertFalse(UsersView::class.java.isInstance(currentView))
    }

    @Test
    fun togglingTwoFactorPersistsOnSave() {
        UI.getCurrent().navigate(UsersView::class.java)

        find<Checkbox>().all().first { it.label == "Require two-factor authentication" }.value = true
        use(shellSave()).click()

        assertEquals(true, connection.getGlobalConfig(false).use2fa)
    }
}
