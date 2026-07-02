package com.jvmguard.ui.components

import com.jvmguard.ui.server.TwoFactor
import com.jvmguard.connector.totp.QRCodeUtil
import com.jvmguard.connector.totp.TOTP
import com.jvmguard.connector.totp.TOTPData
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField

class TwoFactorEnroller(loginName: String) : VerticalLayout() {

    private val totp = TOTPData(TwoFactor.ISSUER, loginName, TOTPData.createSecret())

    val codeField = TextField("Authenticator code").apply {
        width = "10rem"
        testId = ID_CODE
    }

    init {
        isPadding = false
        isSpacing = true
        val intro = Span("Scan this QR code with your authenticator app, then enter the 6-digit code to confirm.")
            .apply { addClassName("jvmguard-field-hint") }
        val qr = Div().apply {
            addClassName("jvmguard-qr")
            element.setProperty("innerHTML", qrSvg())
        }
        val manualKey = Span("Or enter this key manually: ${totp.secretAsBase32}")
            .apply { addClassName("jvmguard-field-hint") }
        add(intro, qr, manualKey, codeField)
    }

    fun verifiedSecretHex(): String? {
        codeField.isInvalid = false
        if (TOTP.validate(totp.secretAsHex, codeField.value.orEmpty())) {
            return totp.secretAsHex
        }
        codeField.errorMessage = "Incorrect authenticator code."
        codeField.isInvalid = true
        return null
    }

    private fun qrSvg(): String = try {
        QRCodeUtil.generateQRCodeSVG(totp.url)
    } catch (_: Exception) {
        ""
    }

    companion object {
        const val ID_CODE = "twofactor-code"
    }
}
