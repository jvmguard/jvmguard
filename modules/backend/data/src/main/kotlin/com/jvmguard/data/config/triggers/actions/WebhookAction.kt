package com.jvmguard.data.config.triggers.actions

open class WebhookAction : TriggerAction() {

    var url: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var httpRequestMethod: HttpRequestMethod = HttpRequestMethod.POST
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var headers: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var bodyContentType: BodyContentType = BodyContentType.JSON
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var formData: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var json: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var timeout: Int = 10
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    private var acceptAllCertificates: Boolean = false

    var isAcceptAllCertificates: Boolean
        get() = acceptAllCertificates
        set(value) {
            val old = acceptAllCertificates
            acceptAllCertificates = value
            fireChanged(old, value)
        }

    override val actionType: ActionType
        get() = ActionType.WEBHOOK

    override val parameterDescription: String
        get() = url
}
