package dev.jvmguard.common

import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

/**
 * Records security-relevant API access (MCP tools, REST endpoints, artifact downloads) to the dedicated
 * "audit" logger.
 *
 * Each entry is a single-line JSON object so a SIEM can read the fields natively.
 */
object AuditLog {

    private val logger = LoggerFactory.getLogger("audit")
    private val mapper = JsonMapper.builder().build()

    enum class Outcome(val warn: Boolean) {
        OK(false),
        ERROR(true),
        DENIED(true),
        AUTH_FAILED(true),
    }

    /**
     * @param source where the call came from, e.g. "mcp" or "rest"
     * @param principal the authenticated login name, or null when unauthenticated (auth failures)
     * @param action the tool name, endpoint, or "download" / "auth"
     * @param outcome OK / ERROR / DENIED / AUTH_FAILED
     * @param target optional subject, e.g. a VM path or artifact id
     * @param detail optional extra context (byte count, error/denial reason, or a structured map describing what a
     *   mutating call changed); a map nests as a JSON object, a string stays a string. Redact secrets before passing.
     * @param clientIp the remote address of the caller, when known
     */
    fun record(
        source: String,
        principal: String?,
        action: String,
        outcome: Outcome,
        target: String? = null,
        detail: Any? = null,
        clientIp: String? = null,
    ) {
        val event = LinkedHashMap<String, Any?>()
        event["time"] = Instant.now().toString()
        event["level"] = if (outcome.warn) "WARN" else "INFO"
        event.putAll(buildEvent(source, principal, action, outcome, target, detail, clientIp))
        val json = mapper.writeValueAsString(event)
        if (outcome.warn) {
            logger.warn(json)
        } else {
            logger.info(json)
        }
    }

    internal fun buildEvent(
        source: String,
        principal: String?,
        action: String,
        outcome: Outcome,
        target: String?,
        detail: Any?,
        clientIp: String?,
    ): Map<String, Any?> = LinkedHashMap<String, Any?>().apply {
        put("source", source)
        put("principal", principal)
        clientIp?.let { put("clientIp", it) }
        put("action", action)
        target?.let { put("target", it) }
        put("outcome", outcome.name.lowercase())
        detail?.let { put("detail", it) }
    }
}
