package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.agent.config.base.ConfigDoc

@ConfigDoc("Invokes an HTTP webhook.")
open class WebhookAction : TriggerAction() {

    @field:ConfigDoc("Target URL invoked when the trigger fires.")
    var url: String = ""
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("HTTP method used for the request.")
    var httpRequestMethod: HttpRequestMethod = HttpRequestMethod.POST
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Extra request headers (newline-separated key=value pairs).")
    var headers: String = ""
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Whether the body is form data or JSON.")
    var bodyContentType: BodyContentType = BodyContentType.JSON
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Request body when bodyContentType=FORM_DATA (key=value lines).")
    var formData: String = ""
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Request body when bodyContentType=JSON.")
    var json: String = ""
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Request timeout in seconds.")
    var timeout: Int = 10
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("If true, skip TLS certificate validation.")
    private var acceptAllCertificates: Boolean = false

    var isAcceptAllCertificates: Boolean
        get() = acceptAllCertificates
        set(value) { acceptAllCertificates = changed(acceptAllCertificates, value) }

    override val actionType: ActionType
        get() = ActionType.WEBHOOK

    override val parameterDescription: String
        get() = url
}
