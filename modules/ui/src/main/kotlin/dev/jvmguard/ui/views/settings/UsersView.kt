package dev.jvmguard.ui.views.settings

import dev.jvmguard.data.config.GlobalConfig
import dev.jvmguard.data.user.Roles
import dev.jvmguard.data.user.User
import dev.jvmguard.ui.components.*
import dev.jvmguard.ui.server.Sessions
import dev.jvmguard.ui.server.StagedListController
import dev.jvmguard.ui.server.enumLabel
import dev.jvmguard.ui.server.t
import dev.jvmguard.ui.shell.MainLayout
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
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route(value = "settings/users", layout = MainLayout::class)
class UsersView : AbstractSettingsSectionView() {

    private val use2fa = Checkbox(t("settings.users.require2fa")).apply {
        testId = ID_USE_2FA
        addClassName("jvmguard-settings-gap-before")
    }
    private val twoFactorHint = Span(t("settings.users.twoFactor.hint")).apply { addClassName("jvmguard-field-hint") }

    private val grid = Grid(User::class.java, false).apply {
        testId = ID_GRID
        addColumn { it.loginName }.setHeader(t("settings.users.edit.loginName")).setAutoWidth(true)
        addColumn { enumLabel(it.userType) }.setHeader(t("settings.sso.type")).setAutoWidth(true)
        addColumn { it.fullName }.setHeader(t("settings.users.edit.fullName")).setAutoWidth(true)
        addColumn { it.email }.setHeader(t("settings.users.edit.email")).setAutoWidth(true)
        addColumn { enumLabel(it.accessLevel) }.setHeader(t("shell.userInfo.accessLevel")).setAutoWidth(true)
        addColumn { Formats.dateTime(it.lastLogin, t("settings.users.never")) }
            .setHeader(t("settings.users.lastLogin")).setAutoWidth(true)
        addComponentColumn { rowActions(it) }.setFlexGrow(0).setAutoWidth(true)
        addItemDoubleClickListener { edit(it.item) }
        editDeleteKeys(::edit, ::confirmDelete)
        addClassName("jvmguard-settings-gap-before")
        setSizeFull()
    }

    init {
        val addUser = Button(t("settings.users.addUser"), VaadinIcon.PLUS.create()) { edit(User()) }.apply {
            addThemeVariants(ButtonVariant.PRIMARY)
            testId = ID_ADD
        }
        val title = H4(t("nav.settings.users"))
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
        menuButton(VaadinIcon.ELLIPSIS_DOTS_V, t("settings.users.row.actions", user.loginName), "$ID_ROW_MENU-${user.loginName}") {
            addItem(t("common.edit")) { edit(user) }
            if (user.apiKeyHash.isNotEmpty()) {
                addItem(t("settings.users.apiKey.revoke")) { confirmRevokeApiKey(user) }
            }
            addItem(t("common.delete")) { confirmDelete(user) }
        }

    private fun confirmRevokeApiKey(user: User) {
        confirm(
            t("settings.users.apiKey.revoke"),
            t("settings.users.apiKey.revokeText", user.loginName),
            t("settings.users.apiKey.revokeButton"),
        ) {
            user.apiKeyHash = ""
            users.markModified(user)
            Notifications.show(t("settings.users.apiKey.revokedNotice", user.loginName))
        }
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
            Notifications.show(t("settings.users.delete.self"))
            return
        }
        if (user.loginName in loggedInUserNames()) {
            Notifications.show(t("settings.users.delete.loggedIn", user.loginName))
            return
        }
        confirm(t("settings.users.delete.title"), t("settings.users.delete.text", user.loginName), t("common.delete")) {
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
