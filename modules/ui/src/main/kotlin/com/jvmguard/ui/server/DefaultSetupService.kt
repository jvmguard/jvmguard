package com.jvmguard.ui.server

import com.jvmguard.data.config.GroupConfig
import com.jvmguard.connector.client.ServerFactory

class DefaultSetupService : SetupService {

    override fun isNewInstallation(): Boolean = try {
        ServerFactory.lookup().isNewInstallation
    } catch (_: Exception) {
        false
    }

    override fun createInitialUser(
        userName: String,
        fullName: String,
        email: String,
        passwordHash: String,
        use2fa: Boolean,
        groupConfig: GroupConfig,
    ) {
        ServerFactory.lookup().createInitialUser(userName, fullName, email, passwordHash, use2fa, null, groupConfig)
    }
}
