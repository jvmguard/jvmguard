package dev.jvmguard.connector.totp

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QRCodeUtil {

    fun generateQRCodeSVG(text: String): String {
        val size = 180
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 0))

        val svgBuilder = StringBuilder()
        svgBuilder.append("<svg width=\"").append(size).append("\" height=\"").append(size).append("\" xmlns=\"http://www.w3.org/2000/svg\">")

        for (y in 0 until bitMatrix.height) {
            for (x in 0 until bitMatrix.width) {
                if (bitMatrix.get(x, y)) {
                    svgBuilder.append("<rect x=\"")
                        .append(x)
                        .append("\" y=\"")
                        .append(y)
                        .append("\" width=\"1\" height=\"1\" fill=\"#000000\"/>")
                }
            }
        }
        svgBuilder.append("</svg>")

        return svgBuilder.toString()
    }
}
