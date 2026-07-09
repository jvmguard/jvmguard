package com.jvmguard.integration.tests.jvmguard.mcp

import com.jvmguard.integration.Controller
import com.jvmguard.integration.Credentials
import com.jvmguard.integration.JvmGuardTest
import com.jvmguard.integration.TestServerConnection
import com.jvmguard.integration.TestVmManager
import java.io.BufferedReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

class McpTest : JvmGuardTest() {

    override fun getServerOptions(runNo: Int, libraryNo: Int): Map<Any, Any> =
        super.getServerOptions(runNo, libraryNo) + mapOf("jvmguard.mcpEnabled" to true)

    private val mcpUrl: java.net.URL get() = URI("http://localhost:$httpPort/mcp").toURL()
    private val apiKey: String get() = Credentials.API_KEY

    override fun connect(
        vmManager: TestVmManager,
        serverConnection: TestServerConnection,
        controller: Controller,
    ) {
        val vms = waitForConnections(serverConnection)
        assert(vms.isNotEmpty()) { "No connected VMs in test" }

        rejectsRequestsWithoutApiKey()
        val sessionId = initializeMcpSession()
        announceInitialized(sessionId)
        listsRegisteredTools(sessionId)
        listVmsReturnsConnectedVm(sessionId, vms.first().hierarchyPath)
        toolCallSucceeds("list_groups", sessionId)
        toolCallSucceeds("list_telemetries", sessionId)
        readsLogFile(sessionId)
        artifactEndpointIsSecured()
        downloadsSnapshotArtifactViaToken(sessionId, vms.first().hierarchyPath)

        println("MCP: All assertions passed")
    }

    private fun rejectsRequestsWithoutApiKey() {
        val response = postJsonRpc(mcpUrl, apiKey = null, body = INITIALIZE_REQUEST)
        assert(response.statusCode == 401) {
            "Expected 401 without auth, got ${response.statusCode}: ${response.body.take(200)}"
        }
        assert(response.headers["www-authenticate"]?.startsWith("Bearer") == true) {
            "401 response should carry a Bearer WWW-Authenticate challenge: ${response.headers["www-authenticate"]}"
        }
        println("MCP: unauthenticated request rejected with Bearer challenge OK")
    }

    private fun initializeMcpSession(): String {
        val response = postJsonRpc(mcpUrl, apiKey = apiKey, body = INITIALIZE_REQUEST)
        assert(response.statusCode == 200) {
            "Expected 200 on initialize, got ${response.statusCode}: ${response.body}"
        }
        assert(response.body.contains("jvmguard")) {
            "Initialize response should identify the 'jvmguard' server: ${response.body}"
        }
        val sessionId = response.headers["mcp-session-id"]
        assert(sessionId != null) { "No Mcp-Session-Id header in initialize response" }
        println("MCP: initialize OK (session=$sessionId)")
        return sessionId!!
    }

    private fun announceInitialized(sessionId: String) {
        postJsonRpc(mcpUrl, apiKey, sessionId, """{"jsonrpc":"2.0","method":"notifications/initialized"}""")
        println("MCP: notifications/initialized OK")
    }

    private fun listsRegisteredTools(sessionId: String) {
        val response = postJsonRpc(mcpUrl, apiKey, sessionId, """{"jsonrpc":"2.0","id":2,"method":"tools/list"}""")
        assert(response.statusCode == 200) {
            "Expected 200 on tools/list, got ${response.statusCode}: ${response.body}"
        }
        for (expectedTool in listOf("list_vms", "list_groups", "get_telemetry", "heap_dump", "get_log_file", "get_snapshot_file")) {
            assert(response.body.contains("\"$expectedTool\"")) {
                "tools/list should advertise '$expectedTool': ${response.body.take(500)}"
            }
        }
        assert(response.body.contains("readOnlyHint")) {
            "tools/list should include tool annotations: ${response.body.take(500)}"
        }
        println("MCP: tools/list OK (${countOccurrences(response.body, "\"name\"")} tools)")
    }

    private fun listVmsReturnsConnectedVm(sessionId: String, expectedVmPath: String) {
        val response = callTool("list_vms", sessionId)
        assert(response.body.contains(expectedVmPath) || response.body.contains("path")) {
            "list_vms response should contain VM data: ${response.body.take(500)}"
        }
        println("MCP: tools/call list_vms OK")
    }

    private fun toolCallSucceeds(toolName: String, sessionId: String) {
        callTool(toolName, sessionId)
        println("MCP: tools/call $toolName OK")
    }

    private fun readsLogFile(sessionId: String) {
        val listResponse = callTool("list_log_files", sessionId)
        // The tool result JSON is embedded as an escaped string inside the MCP envelope, so the quotes
        // around the field name and value are escaped
        val fileName = Regex("""\\"fileName\\":\\"([^\\"]+)""").find(listResponse.body)?.groupValues?.get(1)
        assert(fileName != null) { "list_log_files should return at least one file: ${listResponse.body.take(300)}" }
        val response = postJsonRpc(
            mcpUrl, apiKey, sessionId,
            """{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"get_log_file","arguments":{"fileName":"$fileName","maxLines":10}}}""",
        )
        assert(response.statusCode == 200) { "get_log_file failed: ${response.body.take(300)}" }
        assert(!response.body.contains("\"isError\":true")) { "get_log_file returned an error: ${response.body.take(300)}" }
        println("MCP: tools/call get_log_file OK")
    }

    private fun artifactEndpointIsSecured() {
        val url = URI("http://localhost:$httpPort/mcp-artifacts/999999").toURL()
        assert(artifactDownloadStatus(url, apiKey = null) == 401) {
            "artifact download without auth should be 401"
        }
        assert(artifactDownloadStatus(url, apiKey) == 404) {
            "artifact download of a missing id should be 404"
        }
        println("MCP: artifact endpoint secured (401 without auth, 404 for missing id) OK")
    }

    private fun downloadsSnapshotArtifactViaToken(sessionId: String, vmPath: String) {
        val dump = postJsonRpc(
            mcpUrl, apiKey, sessionId,
            """{"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"heap_dump","arguments":{"vm":"$vmPath"}}}""",
        )
        assert(dump.statusCode == 200 && !dump.body.contains("\"isError\":true")) {
            "heap_dump failed: ${dump.body.take(300)}"
        }

        val snapshotId = waitForSnapshotId(sessionId)
        val getResponse = postJsonRpc(
            mcpUrl, apiKey, sessionId,
            """{"jsonrpc":"2.0","id":9,"method":"tools/call","params":{"name":"get_snapshot_file","arguments":{"id":$snapshotId}}}""",
        )
        assert(getResponse.statusCode == 200) { "get_snapshot_file failed: ${getResponse.body.take(300)}" }
        val downloadUrl = Regex("""\\"downloadUrl\\":\\"([^\\"]+)""").find(getResponse.body)?.groupValues?.get(1)
        assert(downloadUrl != null) { "get_snapshot_file should return a downloadUrl: ${getResponse.body.take(400)}" }

        val (status, bytes) = downloadArtifact(URI(downloadUrl!!).toURL())
        assert(status == 200) { "token download should be 200, got $status" }
        assert(bytes > 0) { "downloaded artifact should have content, got $bytes bytes" }
        println("MCP: snapshot artifact downloaded via single-use token OK ($bytes bytes)")
    }

    private fun waitForSnapshotId(sessionId: String): Long {
        repeat(60) {
            val list = postJsonRpc(
                mcpUrl, apiKey, sessionId,
                """{"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":"list_snapshot_files","arguments":{}}}""",
            )
            Regex("""\\"id\\":(\d+)""").find(list.body)?.let { return it.groupValues[1].toLong() }
            Thread.sleep(1000)
        }
        throw AssertionError("No snapshot file appeared after heap_dump within 60s")
    }

    private fun downloadArtifact(url: java.net.URL): Pair<Int, Long> {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
            connectTimeout = 30000
            readTimeout = 30000
        }
        val code = conn.responseCode
        val bytes = if (code == 200) conn.inputStream.use { it.readBytes().size.toLong() } else 0L
        conn.disconnect()
        return code to bytes
    }

    private fun artifactDownloadStatus(url: java.net.URL, apiKey: String?): Int {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
            if (apiKey != null) setRequestProperty("Authorization", "Bearer $apiKey")
            connectTimeout = 30000
            readTimeout = 30000
        }
        val code = conn.responseCode
        conn.disconnect()
        return code
    }

    private fun callTool(toolName: String, sessionId: String): HttpResponse {
        val response = postJsonRpc(
            mcpUrl, apiKey, sessionId,
            """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"$toolName","arguments":{}}}""",
        )
        assert(response.statusCode == 200) {
            "Expected 200 on $toolName, got ${response.statusCode}: ${response.body}"
        }
        assert(!response.body.contains("\"isError\":true")) {
            "$toolName should not return an error: ${response.body.take(500)}"
        }
        return response
    }

    private data class HttpResponse(
        val statusCode: Int,
        val body: String,
        val headers: Map<String, String>,
    )

    private fun postJsonRpc(
        url: java.net.URL,
        apiKey: String?,
        sessionId: String? = null,
        body: String,
    ): HttpResponse {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json, text/event-stream")
            if (apiKey != null) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            if (sessionId != null) {
                setRequestProperty("Mcp-Session-Id", sessionId)
            }
            connectTimeout = 30000
            readTimeout = 30000
        }
        conn.outputStream.use { os: OutputStream ->
            os.write(body.toByteArray(StandardCharsets.UTF_8))
        }
        val statusCode = conn.responseCode
        val responseHeaders = conn.headerFields.entries
            .filter { it.key != null }
            .associate { it.key!!.lowercase() to it.value.firstOrNull().orEmpty() }
        val bodyText = try {
            val contentType = conn.contentType ?: ""
            val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader(StandardCharsets.UTF_8)?.use { reader: BufferedReader ->
                if (contentType.contains("text/event-stream")) {
                    reader.lineSequence()
                        .filter { it.startsWith("data: ") }
                        .joinToString("") { it.removePrefix("data: ") }
                } else {
                    reader.readText()
                }
            } ?: ""
        } catch (_: Exception) {
            ""
        }
        conn.disconnect()
        return HttpResponse(statusCode, bodyText, responseHeaders)
    }

    private fun countOccurrences(text: String, pattern: String): Int =
        text.windowed(pattern.length) { it.toString() }.count { it == pattern }

    companion object {
        private val INITIALIZE_REQUEST = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
        """.trimIndent()
    }
}
