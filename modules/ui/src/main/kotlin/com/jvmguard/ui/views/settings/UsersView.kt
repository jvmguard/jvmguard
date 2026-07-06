package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.user.Roles
import com.jvmguard.data.user.User
import com.jvmguard.ui.components.*
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.server.StagedListController
import com.jvmguard.ui.shell.MainLayout
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/users", layout = MainLayout::class)
@PageTitle("jvmguard: Settings")
class UsersView : AbstractSettingsSectionView() {

    private val use2fa = Checkbox("Require two-factor authentication").apply {
        testId = ID_USE_2FA
        addClassName("jvmguard-settings-gap-before")
    }
    private val twoFactorHint = Span(
        "Enabling two-factor authentication forces every user without an exemption to enroll an authenticator at their next login.",
    ).apply { addClassName("jvmguard-field-hint") }

    private val grid = Grid(User::class.java, false).apply {
        testId = ID_GRID
        addColumn { it.loginName }.setHeader("Login name").setAutoWidth(true)
        addColumn { it.userType.toString() }.setHeader("Type").setAutoWidth(true)
        addColumn { it.fullName }.setHeader("Full name").setAutoWidth(true)
        addColumn { it.email }.setHeader("Email").setAutoWidth(true)
        addColumn { it.accessLevel.toString() }.setHeader("Access level").setAutoWidth(true)
        addColumn { Formats.dateTime(it.lastLogin, "never") }
            .setHeader("Last login").setAutoWidth(true)
        addComponentColumn { rowActions(it) }.setFlexGrow(0).setAutoWidth(true)
        addItemDoubleClickListener { edit(it.item) }
        editDeleteKeys(::edit, ::confirmDelete)
        addClassName("jvmguard-settings-gap-before")
        setSizeFull()
    }

    init {
        val addUser = Button("Add user", VaadinIcon.PLUS.create()) { edit(User()) }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_ADD
        }
        val title = H4("Users & Roles")
        val header = HorizontalLayout(title, addUser).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            isPadding = false
            expand(title)
        }
        add(header, use2fa, twoFactorHint, grid)
        setFlexGrow(1.0, grid)
    }

    private val users = StagedListController(
        edits = { Sessions.settingsDraft().users },
        load = { serverUsers() },
        markDirty = { Sessions.settingsDraft().markDirty() },
        render = { grid.setItems(it.sortedBy { user -> user.loginName.lowercase() }) },
    )

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        users.reload()
    }

    override fun bind(binder: Binder<GlobalConfig>) {
        binder.forField(use2fa)
            .bind({ it.use2fa }, { config, value -> config.use2fa = value })
    }

    private fun rowActions(user: User): Component =
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, "Actions for ${user.loginName}", "$ID_ROW_MENU-${user.loginName}") {
            addItem("Edit") { edit(user) }
            addItem("Delete") { confirmDelete(user) }
        }

    private fun edit(user: User) {
        val isNew = user.loginName.isEmpty()
        val otherLoginNames = Sessions.settingsDraft().users.items().map { it.loginName }
            .toMutableSet().apply { if (!isNew) remove(user.loginName) }
        UserEditDialog(
            user = user,
            isNew = isNew,
            existingLoginNames = otherLoginNames,
            use2faEnabled = use2fa.value,
            groupPaths = groupPaths(),
            onSave = { saved -> if (isNew) users.add(saved) else users.markModified(saved) },
        ).open()
    }

    private fun confirmDelete(user: User) {
        if (user.loginName == Sessions.current()?.user?.loginName) {
            Notifications.show("You cannot delete the account you are logged in with.")
            return
        }
        if (user.loginName in loggedInUserNames()) {
            Notifications.show("\"${user.loginName}\" is currently logged in and cannot be deleted.")
            return
        }
        confirm("Delete user", "Delete the user \"${user.loginName}\"?", "Delete") {
            users.remove(user)
        }
    }

    private fun loggedInUserNames(): Set<String> =
        Sessions.current()?.serverConnection?.loggedInUsers?.map { it.loginName }?.toSet() ?: emptySet()

    private fun serverUsers(): List<User> =
        Sessions.current()?.serverConnection?.users?.toList() ?: emptyList()

    private fun groupPaths(): List<String> =
        Sessions.current()?.serverConnection?.groupConfigs
            ?.map { it.hierarchyPath }
            ?.filter { it.isNotEmpty() }
            ?.sorted()
            ?: emptyList()

    companion object {
        const val ID_GRID = "users-grid"
        const val ID_ADD = "users-add"
        const val ID_USE_2FA = "users-use-2fa"
        const val ID_ROW_MENU = "user-row-menu"
    }
}
