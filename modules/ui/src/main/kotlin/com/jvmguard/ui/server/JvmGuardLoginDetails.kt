package com.jvmguard.ui.server

import com.jvmguard.connector.api.MockMode
import java.io.Serializable

data class JvmGuardLoginDetails(
    val authenticatorCode: String?,
    val mockMode: MockMode = MockMode.NONE,
) : Serializable {

    companion object {
        @Suppress("unused")
        private const val serialVersionUID = 1L
    }
}
