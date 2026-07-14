package dev.jvmguard.connector.api

import javax.security.auth.login.FailedLoginException

/**
 * User-facing SSO login failure reasons.
 */
enum class SsoLoginError(val code: String, val message: String) {
    NOT_AUTHORIZED("not_authorized", "Access denied. Contact your administrator."),
    ALREADY_REGISTERED("already_registered", "This email address is already registered with a different sign-in method. Please use that method or contact your administrator."),
    DOMAIN_NOT_ALLOWED("domain_not_allowed", "Your email domain is not permitted for single sign-on."),
    EMAIL_NOT_VERIFIED("email_not_verified", "Your email address is not verified with the identity provider."),
    EMAIL_MISSING("email_missing", "The identity provider did not provide an email address."),
    GENERIC("sso_failed", "Single sign-on failed. Please try again or contact your administrator.");

    companion object {
        fun fromCode(code: String?): SsoLoginError? = code?.let { c -> entries.find { it.code == c } }
    }
}

/**
 * Thrown by the server-side SSO authentication to signal user-facing denial.
 */
class SsoLoginException(val error: SsoLoginError) : FailedLoginException(error.message)
