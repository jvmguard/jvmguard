package com.jvmguard.ui.server

import com.jvmguard.common.helper.DeepCopy
import com.jvmguard.data.config.GlobalConfig
import com.jvmguard.data.user.User
import com.jvmguard.ui.views.settings.SettingsArea
import com.jvmguard.connector.api.MockMode
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.QueryParameters
import com.vaadin.flow.server.VaadinSession

object Sessions {

    private const val MOCK_PARAMETER = "mock"
    private const val VM_SELECTION = "vmSelection"
    private const val RECORDING_SELECTION = "recordingSelection"
    private const val SETTINGS_DRAFT = "settingsDraft"
    private const val ACCOUNT_DRAFT = "accountDraft"
    private const val RECORDING_DRAFT = "recordingDraft"
    private const val INSTALLED = "installed"

    @Volatile
    private var loginService: LoginService = DefaultLoginService()

    fun loginService(): LoginService = loginService

    fun setLoginService(service: LoginService) {
        loginService = service
    }

    @Volatile
    private var setupService: SetupService = DefaultSetupService()

    fun setupService(): SetupService = setupService

    fun setSetupService(service: SetupService) {
        setupService = service
    }

    // The "is this a fresh install" answer is cached in the session on first navigation. Tests that swap
    // the setup service after the session already exists clear the cache so the new service is consulted.
    internal fun resetNewInstallationCache() {
        VaadinSession.getCurrent()?.setAttribute(INSTALLED, null)
    }

    fun isNewInstallation(): Boolean {
        val vaadinSession = VaadinSession.getCurrent() ?: return setupService.isNewInstallation()
        if (vaadinSession.getAttribute(INSTALLED) == true) {
            return false
        }
        val fresh = setupService.isNewInstallation()
        if (!fresh) {
            vaadinSession.setAttribute(INSTALLED, true)
        }
        return fresh
    }

    fun captureMock(queryParameters: QueryParameters) {
        val values = queryParameters.parameters[MOCK_PARAMETER]
        val mode = if (values == null) {
            MockMode.NONE
        } else {
            MockMode.fromParameterValue(values.firstOrNull())
        }
        UI.getCurrent()?.let {
            ComponentUtil.setData(it, MOCK_PARAMETER, mode)
        }
    }

    fun mockMode(): MockMode =
        (UI.getCurrent()?.let { ComponentUtil.getData(it, MOCK_PARAMETER) } as? MockMode)
            ?: MockMode.NONE

    fun isMockRequested(): Boolean = mockMode().isMock

    fun current(): UserSession? = VaadinSession.getCurrent()?.getAttribute(UserSession::class.java)

    fun setCurrent(session: UserSession?) {
        VaadinSession.getCurrent().setAttribute(UserSession::class.java, session)
    }

    fun isLoggedIn(): Boolean = current() != null

    fun vmSelectionModel(): VmSelectionModel {
        val ui = UI.getCurrent() ?: return VmSelectionModel()
        return (ComponentUtil.getData(ui, VM_SELECTION) as? VmSelectionModel)
            ?: VmSelectionModel().also { ComponentUtil.setData(ui, VM_SELECTION, it) }
    }

    fun recordingGroupSelection(): VmSelectionModel {
        val ui = UI.getCurrent() ?: return VmSelectionModel()
        return (ComponentUtil.getData(ui, RECORDING_SELECTION) as? VmSelectionModel)
            ?: VmSelectionModel().also { ComponentUtil.setData(ui, RECORDING_SELECTION, it) }
    }

    fun resetRecordingSelection() {
        UI.getCurrent()?.let { ComponentUtil.setData(it, RECORDING_SELECTION, null) }
    }

    fun settingsDraft(): SettingsDraft {
        val ui = UI.getCurrent() ?: return SettingsDraft(GlobalConfig())
        (ComponentUtil.getData(ui, SETTINGS_DRAFT) as? SettingsDraft)?.let { return it }
        val config = current()?.serverConnection?.getGlobalConfig(false)?.let { DeepCopy.clone(it) } ?: GlobalConfig()
        return SettingsDraft(config).also { ComponentUtil.setData(ui, SETTINGS_DRAFT, it) }
    }

    fun peekSettingsDraft(): SettingsDraft? =
        UI.getCurrent()?.let { ComponentUtil.getData(it, SETTINGS_DRAFT) as? SettingsDraft }

    fun clearSettingsDraft() {
        UI.getCurrent()?.let { ComponentUtil.setData(it, SETTINGS_DRAFT, null) }
    }

    fun accountDraft(): AccountDraft {
        val ui = UI.getCurrent() ?: return AccountDraft(User())
        (ComponentUtil.getData(ui, ACCOUNT_DRAFT) as? AccountDraft)?.let { return it }
        val user = current()?.serverConnection?.user?.let { DeepCopy.clone(it) } ?: User()
        return AccountDraft(user).also { ComponentUtil.setData(ui, ACCOUNT_DRAFT, it) }
    }

    fun peekAccountDraft(): AccountDraft? =
        UI.getCurrent()?.let { ComponentUtil.getData(it, ACCOUNT_DRAFT) as? AccountDraft }

    fun clearAccountDraft() {
        UI.getCurrent()?.let { ComponentUtil.setData(it, ACCOUNT_DRAFT, null) }
    }

    fun recordingDraft(): RecordingDraft {
        val ui = UI.getCurrent() ?: return RecordingDraft(emptyList())
        (ComponentUtil.getData(ui, RECORDING_DRAFT) as? RecordingDraft)?.let { return it }
        val groups = current()?.serverConnection?.groupConfigs?.let { DeepCopy.clone(ArrayList(it)) } ?: arrayListOf()
        return RecordingDraft(groups).also { ComponentUtil.setData(ui, RECORDING_DRAFT, it) }
    }

    fun clearRecordingDraft() {
        UI.getCurrent()?.let { ComponentUtil.setData(it, RECORDING_DRAFT, null) }
    }

    fun draft(area: SettingsArea): SettingsModeDraft = when (area) {
        SettingsArea.GENERAL -> settingsDraft()
        SettingsArea.RECORDING -> recordingDraft()
        SettingsArea.ACCOUNT -> accountDraft()
    }

    fun clearDraft(area: SettingsArea) = when (area) {
        SettingsArea.GENERAL -> clearSettingsDraft()
        SettingsArea.RECORDING -> clearRecordingDraft()
        SettingsArea.ACCOUNT -> clearAccountDraft()
    }
}
