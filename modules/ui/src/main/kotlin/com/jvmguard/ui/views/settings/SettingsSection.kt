package com.jvmguard.ui.views.settings

import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.user.AccessLevel
import com.jvmguard.data.user.User
import com.jvmguard.ui.server.Sessions
import com.jvmguard.ui.shell.CachedView
import com.jvmguard.ui.views.login.LoginView
import com.jvmguard.ui.views.vms.VmsView
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.html.H4
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.value.HasValueChangeMode
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver

enum class SettingsArea(val title: String, val savedMessage: String) {
    GENERAL("General Settings", "General settings updated."),
    RECORDING("Recording Settings", "Recording settings updated."),
    ACCOUNT("Account settings", "Account settings updated."),
}

interface SettingsModeView {
    val settingsArea: SettingsArea

    fun applyToDraft()

    fun isValid(): Boolean
}

abstract class AbstractSettingsPage : VerticalLayout(), BeforeEnterObserver, SettingsModeView, CachedView {

    protected open val requiredAccessLevel: AccessLevel get() = AccessLevel.ADMIN

    override val settingsArea: SettingsArea get() = SettingsArea.GENERAL

    init {
        setSizeFull()
        isPadding = false
        isSpacing = false
        addClassName("jvmguard-settings-page")
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!Sessions.isLoggedIn()) {
            event.forwardTo(LoginView::class.java, event.location.queryParameters)
            return
        }
        if (Sessions.current()?.user?.accessLevel?.isAtLeast(requiredAccessLevel) != true) {
            event.forwardTo(VmsView::class.java)
        }
    }

    override fun applyToDraft() {}

    override fun isValid(): Boolean = true
}

abstract class AbstractSettingsSectionView : AbstractSettingsPage() {

    protected val binder: Binder<GlobalConfig> = Binder(GlobalConfig::class.java)
    private var bound = false

    init {
        binder.addStatusChangeListener { if (binder.hasChanges()) Sessions.settingsDraft().markDirty() }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        if (!bound) {
            bind(binder)
            eagerTextFields()
            binder.readBean(Sessions.settingsDraft().config)
            bound = true
        }
    }

    override fun onDetach(detachEvent: DetachEvent) {
        applyToDraft()
        super.onDetach(detachEvent)
    }

    protected abstract fun bind(binder: Binder<GlobalConfig>)

    override fun isValid(): Boolean = binder.validate().isOk

    override fun applyToDraft() {
        Sessions.peekSettingsDraft()?.let { binder.writeBeanIfValid(it.config) }
    }
}

abstract class AbstractAccountSectionView : AbstractSettingsPage() {

    override val requiredAccessLevel: AccessLevel get() = AccessLevel.VIEWER
    override val settingsArea: SettingsArea get() = SettingsArea.ACCOUNT

    protected val binder: Binder<User> = Binder(User::class.java)
    private var bound = false

    init {
        binder.addStatusChangeListener { if (binder.hasChanges()) Sessions.accountDraft().markDirty() }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        if (!bound) {
            bind(binder)
            eagerTextFields()
            binder.readBean(Sessions.accountDraft().user)
            bound = true
        }
    }

    override fun onDetach(detachEvent: DetachEvent) {
        applyToDraft()
        super.onDetach(detachEvent)
    }

    protected abstract fun bind(binder: Binder<User>)

    override fun isValid(): Boolean = binder.validate().isOk

    override fun applyToDraft() {
        Sessions.peekAccountDraft()?.let { binder.writeBeanIfValid(it.user) }
    }
}

private fun Component.eagerTextFields() {
    if (this is HasValueChangeMode) {
        valueChangeMode = ValueChangeMode.EAGER
    }
    children.forEach { it.eagerTextFields() }
}

fun settingsSection(title: String, vararg components: Component): VerticalLayout =
    VerticalLayout(H4(title), *components).apply {
        addClassName("jvmguard-settings-section")
        isPadding = false
        isSpacing = true
        setWidthFull()
    }
