package com.jvmguard.ui.server

import com.jvmguard.data.config.GroupConfig

interface SetupService {

    fun isNewInstallation(): Boolean

    fun createInitialUser(
        userName: String,
        fullName: String,
        email: String,
        passwordHash: String,
        use2fa: Boolean,
        groupConfig: GroupConfig,
    )
}
