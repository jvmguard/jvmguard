package com.jvmguard.data.config.triggers.actions

import com.install4j.runtime.beans.actions.net.HttpRequestMethod as Install4jHttpRequestMethod

enum class ActionType(private val verbose: String, private val actionClass: Class<out TriggerAction>) {
    RECORD_JPS("Record CPU snapshot with JProfiler", RecordJpsAction::class.java),
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
    FORM_DATA("Form data"),
    JSON("JSON");

    override fun toString(): String = verbose
}

@Suppress("unused")
enum class HttpRequestMethod(val httpRequestMethod: Install4jHttpRequestMethod) {
    GET(Install4jHttpRequestMethod.GET),
    POST(Install4jHttpRequestMethod.POST),
    HEAD(Install4jHttpRequestMethod.HEAD),
    OPTIONS(Install4jHttpRequestMethod.OPTIONS),
    PUT(Install4jHttpRequestMethod.PUT),
    DELETE(Install4jHttpRequestMethod.DELETE),
    TRACE(Install4jHttpRequestMethod.TRACE);

    val isBodyFormData: Boolean
        get() = httpRequestMethod.isBodyFormData
}

enum class JfrConfigMode(private val verbose: String) {
    PREDEFINED("Predefined profile"),
    CONFIG_FILE("Config file");

    override fun toString(): String = verbose
}

enum class JfrDefaultProfile(private val verbose: String) {
    DEFAULT("default"),
    PROFILE("profile");

    override fun toString(): String = verbose
}
