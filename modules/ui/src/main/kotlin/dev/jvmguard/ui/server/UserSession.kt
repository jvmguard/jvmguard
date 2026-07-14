package dev.jvmguard.ui.server

import dev.jvmguard.common.notification.ModificationType
import dev.jvmguard.data.config.FrequencyUnit
import dev.jvmguard.data.user.User
import dev.jvmguard.data.user.UserType
import dev.jvmguard.data.user.viewsettings.ViewSettings
import dev.jvmguard.connector.api.ServerConnection
import com.vaadin.flow.server.VaadinService
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

class UserSession(val serverConnection: ServerConnection) {

    private val listeners = CopyOnWriteArrayList<ModificationListener>()

    @Volatile
    var isValid = true
        private set

    val user: User
        get() = serverConnection.user

    private var loadedViewSettings: ViewSettings? = null

    val viewSettings: ViewSettings
        get() = loadedViewSettings
            ?: user.viewSettings.also { loadedViewSettings = it }

    private var loadedFrequencyUnit: FrequencyUnit? = null

    val frequencyUnit: FrequencyUnit
        get() = loadedFrequencyUnit
            ?: serverConnection.getGlobalConfig(false).frequencyUnit.also { loadedFrequencyUnit = it }

    fun saveViewSettings() {
        serverConnection.saveViewSettings(viewSettings)
    }

    @Volatile
    private var setupResolved = false

    fun forcedSetupRequired(): Boolean {
        if (setupResolved) {
            return false
        }
        if (user.userType == UserType.OIDC) {
            setupResolved = true
            return false
        }
        val currentUser = user
        val globalUse2fa = serverConnection.getGlobalConfig(false).use2fa
        val required = TwoFactor.forcedSetupRequired(currentUser, globalUse2fa)
        if (!required) {
            setupResolved = true
        }
        return required
    }

    fun markSetupComplete() {
        setupResolved = true
    }

    fun isLocalRequest(): Boolean = try {
        val request = VaadinService.getCurrentRequest()
        request != null && serverConnection.isLocalAddress(InetAddress.getByName(request.remoteAddr))
    } catch (e: Exception) {
        LOGGER.debug("Could not determine whether the request is local", e)
        false
    }

    fun addModificationListener(listener: ModificationListener) {
        listeners.add(listener)
    }

    fun removeModificationListener(listener: ModificationListener) {
        listeners.remove(listener)
    }

    fun pollAndDispatch(): Boolean {
        if (!isValid) {
            return false
        }
        val modificationTypes: Set<ModificationType> = serverConnection.getAndClearModificationTypes()
        if (modificationTypes.isNotEmpty()) {
            listeners.forEach { it.modifyNotified(modificationTypes) }
        }
        return true
    }

    fun logout() {
        isValid = false
        listeners.clear()
        serverConnection.logout()
    }

    private companion object {
        private val LOGGER = LoggerFactory.getLogger(UserSession::class.java)
    }
}
