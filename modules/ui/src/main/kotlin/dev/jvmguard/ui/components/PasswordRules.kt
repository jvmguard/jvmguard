package dev.jvmguard.ui.components

import dev.jvmguard.common.helper.PasswordHelper
import dev.jvmguard.ui.server.t
import com.vaadin.flow.component.textfield.PasswordField

sealed interface PasswordResult {
    data object Unchanged : PasswordResult

    data object Invalid : PasswordResult

    data class Valid(val plaintext: String) : PasswordResult
}

object PasswordRules {

    const val MIN_LENGTH = 5

    fun validate(
        newField: PasswordField,
        confirmField: PasswordField,
        currentField: PasswordField? = null,
        currentHash: String? = null,
        required: Boolean = false,
    ): PasswordResult {
        listOf(newField, confirmField, currentField).forEach { it?.isInvalid = false }
        val password = newField.value
        if (password.isEmpty()) {
            return if (required) invalid(newField, t("password.enter")) else PasswordResult.Unchanged
        }
        if (password.length < MIN_LENGTH) {
            return invalid(newField, t("password.minLength", MIN_LENGTH))
        }
        if (password != confirmField.value) {
            return invalid(confirmField, t("password.mismatch"))
        }
        if (currentField != null && !PasswordHelper.validatePassword(currentField.value, currentHash ?: "")) {
            return invalid(currentField, t("password.currentIncorrect"))
        }
        return PasswordResult.Valid(password)
    }

    private fun invalid(field: PasswordField, message: String): PasswordResult {
        field.errorMessage = message
        field.isInvalid = true
        return PasswordResult.Invalid
    }
}
