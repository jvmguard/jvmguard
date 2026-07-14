package dev.jvmguard.data.user

import dev.jvmguard.agent.config.base.DefaultConstructor
import dev.jvmguard.data.base.StoredConfig
import dev.jvmguard.data.base.StoredType
import dev.jvmguard.data.user.viewsettings.ViewSettings
import org.jetbrains.annotations.NotNull
import java.time.Instant

@StoredType("user")
open class User @DefaultConstructor constructor() : StoredConfig(), Cloneable, UserSpec {

    override var loginName: String = ""
        set(value) { field = changed(field, value) }

    var userType: UserType = UserType.LOCAL
        set(value) { field = changed(field, value) }

    var ldapDn: String = ""
        set(value) { field = changed(field, value) }

    var ssoIssuer: String = ""
        set(value) { field = changed(field, value) }

    var ssoSubject: String = ""
        set(value) { field = changed(field, value) }

    override var fullName: String = ""
        set(value) { field = changed(field, value) }

    var passwordHash: String? = null
        set(value) { field = changed(field, value) }

    @get:NotNull
    @setparam:NotNull
    var apiKeyHash: String = ""
        set(value) { field = changed(field, value) }

    private var mustChangePassword: Boolean = false
    private var reset2fa: Boolean = false
    private var exemptFrom2fa: Boolean = false
    private var use2fa: Boolean = false

    var isMustChangePassword: Boolean
        get() = mustChangePassword
        set(value) { mustChangePassword = changed(mustChangePassword, value) }

    var isReset2fa: Boolean
        get() = reset2fa
        set(value) { reset2fa = changed(reset2fa, value) }

    var isExemptFrom2fa: Boolean
        get() = exemptFrom2fa
        set(value) { exemptFrom2fa = changed(exemptFrom2fa, value) }

    var isUse2fa: Boolean
        get() = use2fa
        set(value) { use2fa = changed(use2fa, value) }

    var encryptedTotpSecret: String = ""
        set(value) { field = changed(field, value) }

    override var email: String = ""
        set(value) { field = changed(field, value) }

    var accessLevel: AccessLevel = AccessLevel.DEFAULT
        set(value) { field = changed(field, value) }

    var lastLogin: Instant? = null
        set(value) { field = changed(field, value) }

    var groupNames: MutableList<String> = ArrayList()
        set(value) { field = changed(field, value) }

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
