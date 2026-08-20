package dev.jvmguard.ui.components

import dev.jvmguard.ui.server.t
import com.vaadin.flow.data.binder.Validator

object Validators {

    private val EMAIL_PATTERN = Regex("^([a-zA-Z0-9_.\\-+])+@[a-zA-Z0-9\\-.]+\\.[a-zA-Z0-9\\-]{2,}$")

    fun isValidEmail(value: String?): Boolean = !value.isNullOrEmpty() && EMAIL_PATTERN.matches(value)

    fun optionalEmail(message: String = t("validation.email")): Validator<String> =
        Validator.from({ it.isNullOrEmpty() || EMAIL_PATTERN.matches(it) }, message)
}
