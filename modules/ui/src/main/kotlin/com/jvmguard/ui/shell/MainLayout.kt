package com.jvmguard.ui.shell

import com.jvmguard.common.notification.ModificationType
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.ui.components.ErrorDialog
import com.jvmguard.ui.components.Formats
import com.jvmguard.ui.components.Notifications
import com.jvmguard.ui.server.*
import com.jvmguard.ui.views.account.AccountApiKeyView
import com.jvmguard.ui.views.account.AccountProfileView
import com.jvmguard.ui.views.account.AccountTwoFactorView
import com.jvmguard.ui.views.account.AccountView
import com.jvmguard.ui.views.data.mbeans.MBeansView
import com.jvmguard.ui.views.data.telemetry.VmTelemetryView
import com.jvmguard.ui.views.data.transactions.TransactionsView
import com.jvmguard.ui.views.inbox.InboxView
import com.jvmguard.ui.views.log.AuditLogView
import com.jvmguard.ui.views.log.ConnectionLogView
import com.jvmguard.ui.views.log.EventLogView
import com.jvmguard.ui.views.log.LogModeView
import com.jvmguard.ui.views.log.ServerLogView
import com.jvmguard.ui.views.login.AccountSetupView
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.settings.*
import com.jvmguard.ui.views.settings.recording.*
import com.jvmguard.ui.views.setup.InstallWizardView
import com.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.avatar.Avatar
import com.vaadin.flow.component.avatar.AvatarVariant
import com.vaadin.flow.component.badge.Badge
import com.vaadin.flow.component.badge.BadgeVariant
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.contextmenu.MenuItem
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.IconFactory
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.menubar.MenuBarVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.sidenav.SideNav
import com.vaadin.flow.component.sidenav.SideNavItem
import com.vaadin.flow.router.AfterNavigationEvent
import com.vaadin.flow.router.AfterNavigationObserver
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import jakarta.annotation.security.PermitAll
import javax.security.auth.login.CredentialException

@PermitAll
class MainLayout : AppLayout(), BeforeEnterObserver, AfterNavigationObserver, ModificationListener {

    private val userNameValue = Span().apply { addClassName("jvmguard-user-name") }
    private val loginSinceValue = Span().apply { addClassName("jvmguard-user-info-value") }
    private val accessLevelValue = Span().apply { addClassName("jvmguard-user-info-value") }
    private val accountInfo = Div(
        userNameValue,
        userInfoField("Access level", accessLevelValue),
        userInfoField("Logged in since", loginSinceValue),
    ).apply { addClassName("jvmguard-user-info") }

    private val userAvatar = Avatar().apply { addThemeVariants(AvatarVariant.LUMO_SMALL) }
    private val addVmsButton = Button("Add VMs", VaadinIcon.PLUS.create()).apply {
        testId = ID_ADD_VMS
        addClassName("jvmguard-collapsible")
        addClickListener { openAddVms() }
    }
    private lateinit var generalSettingsItem: MenuItem

    private val inboxBadge = Badge().apply {
        testId = ID_INBOX_BADGE
        addClassName("jvmguard-inbox-badge")
        addThemeVariants(BadgeVariant.FILLED, BadgeVariant.SMALL)
        isVisible = false
    }
    private var modificationSession: UserSession? = null

    private val dataNav = buildDataNav()

    private val logEntries = listOf(
        LogNavEntry("Server Log", ServerLogView::class.java, VaadinIcon.FILE_TEXT_O, AccessLevel.ADMIN, ID_LOG_NAV_SERVER),
        LogNavEntry("Audit Log", AuditLogView::class.java, VaadinIcon.CLIPBOARD_CHECK, AccessLevel.ADMIN, ID_LOG_NAV_AUDIT),
        LogNavEntry("Connection Log", ConnectionLogView::class.java, VaadinIcon.CONNECT, AccessLevel.PROFILER, ID_LOG_NAV_CONNECTION),
        LogNavEntry("Event Log", EventLogView::class.java, VaadinIcon.BELL, AccessLevel.VIEWER, ID_LOG_NAV_EVENT),
    )
    private val logNavItems = mutableListOf<Pair<SideNavItem, AccessLevel>>()
    private val logNav = SideNav().apply {
        addClassName("jvmguard-nav")
        logEntries.forEach { entry ->
            val item = SideNavItem(entry.label, entry.view, entry.icon.create()).apply { testId = entry.testId }
            addItem(item)
            logNavItems += item to entry.minLevel
        }
        isVisible = false
    }

    private val generalSettingsEntries = listOf(
        SettingsNavEntry("Users & Roles", UsersView::class.java, VaadinIcon.USERS),
        SettingsNavEntry("LDAP / Active Directory", LdapView::class.java, VaadinIcon.CONNECT),
        SettingsNavEntry("Single Sign-On", SsoView::class.java, VaadinIcon.SIGN_IN_ALT),
        SettingsNavEntry("Data retention", DataSettingsView::class.java, VaadinIcon.DATABASE),
        SettingsNavEntry("E-Mail", SmtpSettingsView::class.java, VaadinIcon.ENVELOPE),
        SettingsNavEntry("Display", DisplaySettingsView::class.java, VaadinIcon.DESKTOP),
        SettingsNavEntry("Updates", UpdatesSettingsView::class.java, VaadinIcon.DOWNLOAD),
        SettingsNavEntry("Import / Export", ImportExportView::class.java, VaadinIcon.FILE_TEXT),
    )
    private val recordingSettingsEntries = listOf(
        SettingsNavEntry("Transactions", RecordingTransactionsView::class.java, VaadinIcon.EXCHANGE),
        SettingsNavEntry("Telemetries", RecordingTelemetriesView::class.java, VaadinIcon.CHART),
        SettingsNavEntry("Thresholds", RecordingThresholdsView::class.java, VaadinIcon.DASHBOARD),
        SettingsNavEntry("Triggers", RecordingTriggersView::class.java, VaadinIcon.BOLT),
    )
    private val accountEntries = listOf(
        SettingsNavEntry("User information", AccountProfileView::class.java, VaadinIcon.USER),
        SettingsNavEntry("Two-factor authentication", AccountTwoFactorView::class.java, VaadinIcon.SHIELD),
        SettingsNavEntry("External access", AccountApiKeyView::class.java, VaadinIcon.KEY),
    )
    private val generalSettingsNav = buildNav(generalSettingsEntries).apply { isVisible = false }
    private val recordingSettingsNav = buildNav(recordingSettingsEntries).apply { isVisible = false }
    private val accountNav = buildNav(accountEntries).apply { isVisible = false }
    private val areaNavs = mapOf(
        SettingsArea.GENERAL to generalSettingsNav,
        SettingsArea.RECORDING to recordingSettingsNav,
        SettingsArea.ACCOUNT to accountNav,
    )
    private val drawerToggle = DrawerToggle().apply { setAriaLabel("Toggle navigation") }

    private val settingsSave = Button("Save") { saveSettings() }.apply {
        addThemeVariants(ButtonVariant.PRIMARY)
        addClassName("jvmguard-settings-save")
        testId = ID_SETTINGS_SAVE
    }

    private val appHeader: HorizontalLayout
    private val settingsHeader: HorizontalLayout
    private val logHeader: HorizontalLayout
    private var userMenu: MenuBar
    private var settingsMenu: MenuBar

    private var currentArea: SettingsArea? = null
    private var currentSection: SettingsModeView? = null
    private var currentMode = ShellMode.DATA
    private var poller: NotificationPoller? = null

    private val settingsTitle = H3("General Settings").apply { addClassName("jvmguard-settings-title") }
    private val logTitle = H3("Logs").apply { addClassName("jvmguard-settings-title") }

    init {
        addClassName("jvmguard-shell")
        addToDrawer(dataNav)
        addToDrawer(generalSettingsNav)
        addToDrawer(recordingSettingsNav)
        addToDrawer(accountNav)
        addToDrawer(logNav)

        val logo = Div().apply { addClassName("jvmguard-logo") }
        userMenu = buildUserMenu()
        settingsMenu = buildSettingsMenu()
        appHeader = HorizontalLayout(logo, addVmsButton, ThemeToggle(), userMenu, settingsMenu).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            expand(logo)
        }

        val cancel = Button("Cancel") { cancelSettings() }.apply { testId = ID_SETTINGS_CANCEL }
        val settingsLogo = Div().apply { addClassName("jvmguard-logo") }
        val separator = Div().apply { addClassName("jvmguard-settings-separator") }
        val titleGroup = HorizontalLayout(settingsLogo, separator, settingsTitle).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isPadding = false
            isSpacing = false
        }
        settingsHeader = HorizontalLayout(titleGroup, cancel, settingsSave).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            expand(titleGroup)
            isVisible = false
        }

        val logClose = Button("Close") { closeLogs() }.apply { testId = ID_LOG_CLOSE }
        val logLogo = Div().apply { addClassName("jvmguard-logo") }
        val logSeparator = Div().apply { addClassName("jvmguard-settings-separator") }
        val logTitleGroup = HorizontalLayout(logLogo, logSeparator, logTitle).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            isPadding = false
            isSpacing = false
        }
        logHeader = HorizontalLayout(logTitleGroup, logClose).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            expand(logTitleGroup)
            isVisible = false
        }

        addToNavbar(
            HorizontalLayout(drawerToggle, appHeader, settingsHeader, logHeader).apply {
                addClassName("jvmguard-header")
                defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
                setWidthFull()
                setFlexGrow(1.0, appHeader)
                setFlexGrow(1.0, settingsHeader)
                setFlexGrow(1.0, logHeader)
            },
        )
    }

    private fun buildDataNav(): SideNav = SideNav().apply {
        addClassName("jvmguard-nav")
        fun item(label: String, viewClass: Class<out Component>, icon: IconFactory) =
            addItem(SideNavItem(label, viewClass, icon.create()))
        item("VMs", VmsView::class.java, VaadinIcon.SERVER)
        item("Transactions", TransactionsView::class.java, VaadinIcon.EXCHANGE)
        item("Telemetries", VmTelemetryView::class.java, VaadinIcon.CHART)
        item("MBeans", MBeansView::class.java, VaadinIcon.CUBES)
        addItem(SideNavItem("Inbox", InboxView::class.java, VaadinIcon.INBOX.create()).apply {
            suffixComponent = inboxBadge
        })
    }

    private fun buildNav(entries: List<SettingsNavEntry>): SideNav = SideNav().apply {
        addClassName("jvmguard-nav")
        entries.forEach { addItem(SideNavItem(it.label, it.view, it.icon.create())) }
    }

    private fun areaEntries(area: SettingsArea): List<SettingsNavEntry> = when (area) {
        SettingsArea.GENERAL -> generalSettingsEntries
        SettingsArea.RECORDING -> recordingSettingsEntries
        SettingsArea.ACCOUNT -> accountEntries
    }

    private fun buildUserMenu(): MenuBar = MenuBar().apply {
        addThemeVariants(MenuBarVariant.LUMO_TERTIARY, MenuBarVariant.LUMO_ICON)
        testId = ID_USER_MENU
        addClassName("jvmguard-collapsible")
        val root = addItem(userAvatar).apply { element.setAttribute("aria-label", "Account") }
        root.subMenu.apply {
            addComponent(accountInfo)
            addSeparator()
            addItem("Account settings") { UI.getCurrent().navigate(AccountView::class.java) }.apply { testId = ID_ACCOUNT }
            addItem("Log out") { logout() }.apply { testId = ID_LOGOUT }
        }
    }

    private fun buildSettingsMenu(): MenuBar = MenuBar().apply {
        addThemeVariants(MenuBarVariant.LUMO_TERTIARY, MenuBarVariant.LUMO_ICON)
        testId = ID_SETTINGS
        addClassName("jvmguard-collapsible")
        addClassName("jvmguard-settings-cog")
        isVisible = false // shown for profiler+ in beforeEnter
        val cog = addItem(VaadinIcon.COG.create().apply { setSize("1.25rem") }).apply {
            element.setAttribute("aria-label", "Settings")
            element.setAttribute("title", "Settings")
        }
        generalSettingsItem = cog.subMenu.addItem("General Settings") { UI.getCurrent().navigate(SettingsView::class.java) }
            .apply { testId = ID_GENERAL_SETTINGS }
        cog.subMenu.addItem("Recording Settings") { UI.getCurrent().navigate(RecordingSettingsView::class.java) }
            .apply { testId = ID_RECORDING_SETTINGS }
        cog.subMenu.addItem("Logs") { openLogs() }.apply { testId = ID_LOGS }
    }

    private fun userInfoField(label: String, value: Span): Div =
        Div(Span(label).apply { addClassName("jvmguard-user-info-label") }, value)
            .apply { addClassName("jvmguard-user-info-field") }

    override fun afterNavigation(event: AfterNavigationEvent) {
        val view = event.activeChain.firstOrNull()
        currentSection = view as? SettingsModeView
        val logView = view as? LogModeView
        val area = currentSection?.settingsArea
        val mode = when {
            logView != null -> ShellMode.LOG
            area != null -> ShellMode.SETTINGS
            else -> ShellMode.DATA
        }
        if (mode != currentMode || area != currentArea) {
            Notifications.closeAll()
        }
        currentMode = mode
        currentArea = area
        dataNav.isVisible = mode == ShellMode.DATA
        appHeader.isVisible = mode == ShellMode.DATA
        settingsHeader.isVisible = mode == ShellMode.SETTINGS
        logHeader.isVisible = mode == ShellMode.LOG
        logNav.isVisible = mode == ShellMode.LOG
        areaNavs.forEach { (navArea, nav) -> nav.isVisible = mode == ShellMode.SETTINGS && navArea == area }
        if (area != null) {
            settingsTitle.text = area.title
            // Save stays disabled until the area's draft is changed (V1's change detection).
            val draft = Sessions.draft(area)
            settingsSave.isEnabled = draft.dirty
            draft.onDirty { settingsSave.isEnabled = true }
        }
        if (logView != null) {
            logTitle.text = logView.logTitle
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        Sessions.captureMock(event.location.queryParameters)
        // In production Spring Security's navigation access control redirects unauthenticated users to
        // the login view before this layout is ever entered (route access lives in the @PermitAll/
        // @RolesAllowed annotations). This guard is the fallback for environments where that filter
        // chain is not engaged (the browserless Karibu tests) and defense-in-depth otherwise.
        if (!Sessions.isLoggedIn()) {
            val target = if (Sessions.isNewInstallation()) InstallWizardView::class.java else LoginView::class.java
            event.forwardTo(target, event.location.queryParameters)
            return
        }
        if (Sessions.current()?.forcedSetupRequired() == true) {
            event.forwardTo(AccountSetupView::class.java)
            return
        }
        val user = Sessions.current()?.user
        userAvatar.name = userName()
        userNameValue.text = userName()
        loginSinceValue.text = Formats.dateTime(user?.lastLogin)
        accessLevelValue.text = user?.accessLevel?.toString().orEmpty()
        settingsMenu.isVisible = user?.accessLevel?.isAtLeast(AccessLevel.PROFILER) == true
        generalSettingsItem.isVisible = user?.accessLevel?.isAtLeast(AccessLevel.ADMIN) == true
        logNavItems.forEach { (item, minLevel) -> item.isVisible = user?.accessLevel?.isAtLeast(minLevel) == true }
        addVmsButton.isEnabled = user?.accessLevel?.isAtLeast(AccessLevel.PROFILER) == true
    }

    private fun userName(): String {
        val user = Sessions.current()?.user ?: return ""
        return user.fullName.takeIf { it.isNotEmpty() } ?: user.loginName
    }

    private fun saveSettings() {
        val area = currentArea ?: return
        val ui = UI.getCurrent() ?: return
        val order = areaEntries(area).map { it.view }
        val sections = (KeepAliveInstantiator.instances(ui).filterIsInstance<SettingsModeView>() + listOfNotNull(currentSection))
            .distinct()
            .filter { it.settingsArea == area }
            .sortedBy { order.indexOf((it as Component).javaClass) }
        sections.firstOrNull { !it.isValid() }?.let { invalid ->
            ui.navigate((invalid as Component).javaClass)
            Notifications.closeAll()
            Notifications.show("Please correct the highlighted fields before saving.")
            return
        }
        sections.forEach { it.applyToDraft() }
        try {
            Sessions.current()?.serverConnection?.let { Sessions.draft(area).persist(it) }
        } catch (e: CredentialException) {
            ErrorDialog("Could not save settings", e.message ?: e.toString(), null).open()
            return
        }
        resetArea(area)
        // Navigate first so that the notification survives.
        UI.getCurrent().navigate(VmsView::class.java)
        Notifications.show(area.savedMessage)
    }

    private fun cancelSettings() {
        currentArea?.let { resetArea(it) }
        UI.getCurrent().navigate(VmsView::class.java)
    }

    private fun openLogs() {
        val level = Sessions.current()?.user?.accessLevel
        val target = when {
            level?.isAtLeast(AccessLevel.ADMIN) == true -> ServerLogView::class.java
            level?.isAtLeast(AccessLevel.PROFILER) == true -> ConnectionLogView::class.java
            else -> EventLogView::class.java
        }
        UI.getCurrent().navigate(target)
    }

    private fun closeLogs() {
        UI.getCurrent().navigate(VmsView::class.java)
    }

    private fun resetArea(area: SettingsArea) {
        Sessions.clearDraft(area)
        UI.getCurrent()?.let { ui ->
            KeepAliveInstantiator.evict(ui) { it is SettingsModeView && it.settingsArea == area }
        }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        Sessions.current()?.let { session ->
            poller = NotificationPoller.start(attachEvent.ui, session)
            modificationSession = session
            session.addModificationListener(this)
            refreshInboxBadge()
        }
    }

    override fun onDetach(detachEvent: DetachEvent) {
        poller?.stop()
        poller = null
        modificationSession?.removeModificationListener(this)
        modificationSession = null
        super.onDetach(detachEvent)
    }

    override fun modifyNotified(modificationTypes: Set<ModificationType>) {
        if (ModificationType.INBOX in modificationTypes) {
            refreshInboxBadge()
        }
    }

    private fun refreshInboxBadge() {
        val unread = Sessions.current()?.serverConnection?.inboxItems?.count { !it.isItemRead } ?: 0
        showInboxUnread(unread)
    }

    fun showInboxUnread(unread: Int) {
        inboxBadge.text = unread.toString()
        inboxBadge.isVisible = unread > 0
    }

    private fun logout() {
        SecurityBridge.logout()
    }

    private enum class ShellMode { DATA, SETTINGS, LOG }

    private class SettingsNavEntry(val label: String, val view: Class<out Component>, val icon: VaadinIcon)

    private class LogNavEntry(
        val label: String,
        val view: Class<out Component>,
        val icon: VaadinIcon,
        val minLevel: AccessLevel,
        val testId: String,
    )

    companion object {
        const val ID_USER_MENU = "user-menu"
        const val ID_ADD_VMS = "add-vms-button"
        const val ID_SETTINGS = "settings-button"
        const val ID_GENERAL_SETTINGS = "general-settings-menu-item"
        const val ID_RECORDING_SETTINGS = "recording-settings-menu-item"
        const val ID_LOGS = "logs-menu-item"
        const val ID_INBOX_BADGE = "inbox-badge"
        const val ID_LOG_CLOSE = "log-close"
        const val ID_LOG_NAV_SERVER = "log-nav-server"
        const val ID_LOG_NAV_AUDIT = "log-nav-audit"
        const val ID_LOG_NAV_CONNECTION = "log-nav-connection"
        const val ID_LOG_NAV_EVENT = "log-nav-event"
        const val ID_ACCOUNT = "account-menu-item"
        const val ID_LOGOUT = "logout"
        const val ID_SETTINGS_SAVE = "settings-save"
        const val ID_SETTINGS_CANCEL = "settings-cancel"
    }
}
