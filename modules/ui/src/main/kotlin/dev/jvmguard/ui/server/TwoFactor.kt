package dev.jvmguard.ui.server

import dev.jvmguard.data.user.User

object TwoFactor {

    const val ISSUER = "jvmguard"

    fun enrollmentRequired(user: User, globalUse2fa: Boolean): Boolean =
        globalUse2fa && ((user.isReset2fa && user.isUse2fa) || (!user.isExemptFrom2fa && !user.isUse2fa))

    fun forcedSetupRequired(user: User, globalUse2fa: Boolean): Boolean =
        user.isMustChangePassword || enrollmentRequired(user, globalUse2fa)
}
