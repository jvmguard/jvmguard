package com.jvmguard.collector.main

import com.install4j.runtime.beans.KeyValuePair
import com.install4j.runtime.beans.actions.net.HttpRequestAction
import com.install4j.runtime.installer.helper.content.HttpConnection
import com.install4j.runtime.installer.helper.content.HttpRequestHandler
import com.install4j.runtime.installer.helper.content.TextRequestHandler
import com.jvmguard.common.Loggers
import com.jvmguard.data.config.triggers.actions.BodyContentType
import com.jvmguard.data.config.triggers.actions.KeyValuePairHelper
import com.jvmguard.data.config.triggers.actions.WebhookAction
import java.io.IOException

object WebhookHelper {

    const val TRIGGER_TOKEN = "@TRIGGER@"

    private val SERVER_LOGGER = Loggers.SERVER

    fun newWebhookRunnable(webhookAction: WebhookAction, triggeredBy: String) = Runnable {
        val replacements = hashMapOf(TRIGGER_TOKEN to triggeredBy)
        try {
            TextRequestHandler(HttpRequestHandler.MODE_UNATTENDED).use { handler ->
                handler.apply {
                    setRequestHeaders(createRequestHeadersMap(parseKeyValuePairs(webhookAction.headers, replacements)))
                    setConnectTimeout(webhookAction.timeout * 1000)
                    setReadTimeout(webhookAction.timeout * 1000)
                    setAcceptAllCertificates(webhookAction.isAcceptAllCertificates)
                }

                val httpRequestMethod = webhookAction.httpRequestMethod
                val formData: List<KeyValuePair>
                val useRequestBody: Boolean
                if (!httpRequestMethod.isBodyFormData || webhookAction.bodyContentType == BodyContentType.FORM_DATA) {
                    formData = parseKeyValuePairs(webhookAction.formData, replacements)
                    useRequestBody = false
                } else {
                    formData = emptyList()
                    useRequestBody = true
                }
                HttpRequestAction.performRequest(
                    handler,
                    webhookAction.url,
                    httpRequestMethod.httpRequestMethod,
                    formData,
                    useRequestBody,
                    "application/json",
                    replace(webhookAction.json, replacements),
                )
                SERVER_LOGGER.info("invoked webhook \"{}\" with status code {}", webhookAction.url, getStatusCode(handler.connection))
            }
        } catch (e: Throwable) {
            SERVER_LOGGER.error("could not invoke webhook \"{}\"", webhookAction.url, e)
        }
    }

    private fun getStatusCode(connection: HttpConnection): String =
        try {
            connection.responseCode.toString()
        } catch (_: IOException) {
            "<error>"
        }

    private fun parseKeyValuePairs(text: String, replacements: Map<String, String>): List<KeyValuePair> =
        KeyValuePairHelper.parseKeyValuePairs(text).onEach { it.value = replace(it.value, replacements) }

    private fun createRequestHeadersMap(keyValuePairs: List<KeyValuePair>): Map<String, List<String>> =
        keyValuePairs.groupBy({ it.key }, { it.value })

    private fun replace(value: String, replacements: Map<String, String>): String =
        replacements.entries.fold(value) { result, (key, replacement) -> result.replace(key, replacement) }
}
