package com.jvmguard.data.user

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.data.base.StoredConfig
import com.jvmguard.data.base.StoredType
import com.jvmguard.data.user.viewsettings.ViewSettings
import org.jetbrains.annotations.NotNull
import java.time.Instant

@StoredType("user")
open class User @DefaultConstructor constructor() : StoredConfig(), Cloneable, UserSpec {

    override var loginName: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var userType: UserType = UserType.LOCAL
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var ldapDn: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var ssoIssuer: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var ssoSubject: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    override var fullName: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var passwordHash: String? = null
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @get:NotNull
    @setparam:NotNull
    var apiKeyHash: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    private var mustChangePassword: Boolean = false
    private var reset2fa: Boolean = false
    private var exemptFrom2fa: Boolean = false
    private var use2fa: Boolean = false

    var isMustChangePassword: Boolean
        get() = mustChangePassword
        set(value) {
            val old = mustChangePassword
            mustChangePassword = value
            fireChanged(old, value)
        }

    var isReset2fa: Boolean
        get() = reset2fa
        set(value) {
            val old = reset2fa
            reset2fa = value
            fireChanged(old, value)
        }

    var isExemptFrom2fa: Boolean
        get() = exemptFrom2fa
        set(value) {
            val old = exemptFrom2fa
            exemptFrom2fa = value
            fireChanged(old, value)
        }

    var isUse2fa: Boolean
        get() = use2fa
        set(value) {
            val old = use2fa
            use2fa = value
            fireChanged(old, value)
        }

    var encryptedTotpSecret: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    override var email: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var accessLevel: AccessLevel = AccessLevel.DEFAULT
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var lastLogin: Instant? = null
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var groupNames: MutableList<String> = ArrayList()
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var viewSettings: ViewSettings = ViewSettings()

    constructor(
        loginName: String,
        fullName: String,
        passwordHash: String?,
        email: String,
        accessLevel: AccessLevel,
    ) : this() {
        this.loginName = loginName
        this.fullName = fullName
        this.passwordHash = passwordHash
        this.email = email
        this.accessLevel = accessLevel
        resetModified()
    }

    fun copyFromRestricted(modifiedUser: User) {
        userType = modifiedUser.userType
        fullName = modifiedUser.fullName
        email = modifiedUser.email
        passwordHash = modifiedUser.passwordHash
        apiKeyHash = modifiedUser.apiKeyHash
        isMustChangePassword = modifiedUser.isMustChangePassword
        isReset2fa = modifiedUser.isReset2fa
        isExemptFrom2fa = modifiedUser.isExemptFrom2fa
        isUse2fa = modifiedUser.isUse2fa
        encryptedTotpSecret = modifiedUser.encryptedTotpSecret
        ldapDn = modifiedUser.ldapDn
        ssoIssuer = modifiedUser.ssoIssuer
        ssoSubject = modifiedUser.ssoSubject
    }

    public override fun clone(): User {
        val clone = super.clone() as User
        clone.groupNames = ArrayList(groupNames)
        return clone
    }
}

interface UserSpec {
    var loginName: String
    var fullName: String
    var email: String
}

enum class UserType(private val verbose: String) {
    LOCAL("Local"),
    LDAP("LDAP"),
    OIDC("SSO");

    override fun toString(): String = verbose
}
