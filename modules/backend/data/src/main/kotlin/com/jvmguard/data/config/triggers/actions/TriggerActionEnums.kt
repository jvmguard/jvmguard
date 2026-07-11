package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.ConfigDoc
import com.install4j.runtime.beans.actions.net.HttpRequestMethod as Install4jHttpRequestMethod

enum class ActionType(private val verbose: String, private val actionClass: Class<out TriggerAction>) {
    RECORD_JPS("Record JProfiler snapshot", RecordJpsAction::class.java),
    RECORD_JFR("Record JDK Flight Recorder snapshot", RecordJfrAction::class.java),
    THREAD_DUMP("Save thread dump", ThreadDumpAction::class.java),
    HEAP_DUMP("Save HPROF memory snapshot", HeapDumpAction::class.java),
    EMAIL("Send email", EmailAction::class.java),
    WEBHOOK("Invoke webhook", WebhookAction::class.java),
    LOG("Write log file entry", LogAction::class.java),
    INBOX("Create inbox entry", InboxAction::class.java);

    fun createAction(): TriggerAction = actionClass.getDeclaredConstructor().newInstance()

    override fun toString(): String = verbose
}

enum class BodyContentType(private val verbose: String) {
    @ConfigDoc("Request body is form data.")
    FORM_DATA("Form data"),
    @ConfigDoc("Request body is JSON.")
    JSON("JSON");

    override fun toString(): String = verbose
}

@Suppress("unused")
enum class HttpRequestMethod(val httpRequestMethod: Install4jHttpRequestMethod) {
    @ConfigDoc("HTTP GET request.")
    GET(Install4jHttpRequestMethod.GET),
    @ConfigDoc("HTTP POST request.")
    POST(Install4jHttpRequestMethod.POST),
    @ConfigDoc("HTTP HEAD request.")
    HEAD(Install4jHttpRequestMethod.HEAD),
    @ConfigDoc("HTTP OPTIONS request.")
    OPTIONS(Install4jHttpRequestMethod.OPTIONS),
    @ConfigDoc("HTTP PUT request.")
    PUT(Install4jHttpRequestMethod.PUT),
    @ConfigDoc("HTTP DELETE request.")
    DELETE(Install4jHttpRequestMethod.DELETE),
    @ConfigDoc("HTTP TRACE request.")
    TRACE(Install4jHttpRequestMethod.TRACE);

    val isBodyFormData: Boolean
        get() = httpRequestMethod.isBodyFormData
}

enum class JfrConfigMode(private val verbose: String) {
    @ConfigDoc("Use a predefined JFR profile.")
    PREDEFINED("Predefined profile"),
    @ConfigDoc("Use an explicit JFR config file.")
    CONFIG_FILE("Config file");

    override fun toString(): String = verbose
}

enum class JfrDefaultProfile(private val verbose: String) {
    DEFAULT("default"),
    PROFILE("profile");

    override fun toString(): String = verbose
}
