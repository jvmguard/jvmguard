package com.jvmguard.ui.components

import com.jvmguard.common.helper.PasswordHelper
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
            return if (required) invalid(newField, "Enter a password.") else PasswordResult.Unchanged
        }
        if (password.length < MIN_LENGTH) {
            return invalid(newField, "At least $MIN_LENGTH characters.")
        }
        if (password != confirmField.value) {
            return invalid(confirmField, "The passwords do not match.")
        }
        if (currentField != null && !PasswordHelper.validatePassword(currentField.value, currentHash ?: "")) {
            return invalid(currentField, "Your current password is incorrect.")
        }
        return PasswordResult.Valid(password)
    }

    private fun invalid(field: PasswordField, message: String): PasswordResult {
        field.errorMessage = message
        field.isInvalid = true
        return PasswordResult.Invalid
    }
}
