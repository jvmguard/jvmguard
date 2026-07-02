package com.jvmguard.ui.server

import com.jvmguard.common.helper.PasswordHelper
import com.jvmguard.data.user.User
import com.jvmguard.connector.api.ServerConnection

class AccountDraft(val user: User) : SettingsModeDraft {

    override var dirty = false
        private set

    private var onDirty: (() -> Unit)? = null

    var pendingApiKey: String? = null

    var pendingTotpSecretHex: String? = null

    var pendingUse2fa: Boolean? = null

    override fun markDirty() {
        if (!dirty) {
            dirty = true
            onDirty?.invoke()
        }
    }

    override fun onDirty(listener: () -> Unit) {
        onDirty = listener
    }

    override fun persist(connection: ServerConnection) {
        pendingApiKey?.let { user.apiKeyHash = PasswordHelper.createHash(it) }
        pendingTotpSecretHex?.let { user.encryptedTotpSecret = connection.encryptTotpSecret(it) }
        pendingUse2fa?.let {
            user.isUse2fa = it
            if (!it) {
                user.encryptedTotpSecret = ""
            }
        }
        connection.saveSelf(user)
    }
}
