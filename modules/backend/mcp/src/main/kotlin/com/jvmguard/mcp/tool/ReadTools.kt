package com.jvmguard.mcp.tool

import com.jvmguard.connector.api.log.LogFileType
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.spec.McpSchema.Tool
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification

class ListSnapshotFilesTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "list_snapshot_files"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path. Omit for all VMs."),
                    "type" to stringProperty(
                        "Filter by snapshot type.",
                        SnapshotFileType.entries.map { it.name },
                    ),
                    "since" to integerProperty(
                        "Only return artifacts created at or after this epoch-millis time. Pass the triggeredAt " +
                                "from a capture tool to isolate the artifact you just triggered."
                    ),
                    "limit" to integerProperty("Maximum number of files to return. Default: 200."),
                ),
            ),
        ).description(
            "List captured diagnostic artifacts (memory snapshots, CPU snapshots, thread dumps, JFR recordings), " +
                    "newest first. These are produced by heap_dump, thread_dump, record_jfr, record_jps, or by " +
                    "triggers. Retrieve one with get_snapshot_file."
        ).annotations(readOnly("List snapshot files")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as? String
                val typeStr = args["type"] as? String
                val since = (args["since"] as? Number)?.toLong()
                val limit = (args["limit"] as? Number)?.toInt() ?: 200
                val type = typeStr?.let { runCatching { SnapshotFileType.valueOf(it) }.getOrNull() }
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVmOrNull(conn, vmPath)
                    val files = conn.getSnapshotFiles(type, vm)
                        .filter { since == null || it.dateCreated.toEpochMilli() >= since }
                        .sortedByDescending { it.dateCreated }
                        .take(limit)
                        .map { f ->
                        mapOf(
                            "id" to f.id,
                            // Pool members have a trailing slash which needs to be normalized.
                            "vm" to f.vm.hierarchyPath.trimEnd('/'),
                            "instance" to f.vm.name,
                            "type" to f.type.name,
                            "name" to f.name,
                            "dateCreated" to f.dateCreated.toString(),
                            "size" to f.uncompressedLength,
                        )
                    }
                    jsonResult(McpJson.write(files))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}

class ListMbeansTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "list_mbeans"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path (from list_vms)."),
                    "includePlatform" to booleanProperty(
                        "If true, include platform MBeans (java.lang:* etc.). Default: true."
                    ),
                    "limit" to integerProperty("Maximum number of names to return. Default: 500."),
                ),
                listOf("vm"),
            ),
        ).description(
            "List MBean object names registered in the target VM's MBean server. " +
                    "Use the returned names in get_mbean_data to inspect attributes. " +
                    "A pool path resolves to one connected member, since a pool has no MBean server of its own."
        ).annotations(readOnly("List MBeans")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as String
                val includePlatform = (args["includePlatform"] as? Boolean) ?: true
                val limit = (args["limit"] as? Number)?.toInt() ?: 500
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveLiveVm(conn, vmPath)
                    val names = conn.getMBeanNames(vm, includePlatform).take(limit)
                    jsonResult(McpJson.write(names))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}

class GetMbeanDataTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "get_mbean_data"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path (a pool path resolves to one connected member)."),
                    "name" to stringProperty("MBean object name (from list_mbeans), e.g. \"java.lang:type=Memory\"."),
                    "fetchValues" to booleanProperty(
                        "If true (default), return each attribute's current value. " +
                                "If false, return each attribute's declared type instead (schema only)."
                    ),
                ),
                listOf("vm", "name"),
            ),
        ).description(
            "Read a specific MBean. Returns { objectName, attributes, operations }. With fetchValues=true " +
                    "(default), 'attributes' maps each attribute name to its current value, decoded into native " +
                    "JSON (composites become objects, arrays become lists, simple-key tabular data becomes a map). " +
                    "With fetchValues=false, 'attributes' maps each name to its declared type instead. " +
                    "'operations' lists invocable operation signatures such as \"listStores(int)\"."
        ).annotations(readOnly("Get MBean data")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as String
                val name = args["name"] as String
                val fetchValues = (args["fetchValues"] as? Boolean) ?: true
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveLiveVm(conn, vmPath)
                    val data = conn.getMBeanData(vm, name, true, fetchValues)
                    val beanInfo = data?.beanInfo
                    if (beanInfo == null || (beanInfo.attributes.isEmpty() && beanInfo.operations.isEmpty())) {
                        errorResult("MBean not found: $name")
                    } else {
                        val attributes = LinkedHashMap<String, Any?>()
                        beanInfo.attributes.forEachIndexed { index, attribute ->
                            attributes[attribute.name] = if (fetchValues) {
                                McpMBeanData.decodeAttribute(attribute, data.values.getOrNull(index))
                            } else {
                                attribute.type
                            }
                        }
                        val result = buildMap {
                            put("objectName", name)
                            put("attributes", attributes)
                            val operations = McpMBeanData.operationSignatures(beanInfo)
                            if (operations.isNotEmpty()) {
                                put("operations", operations)
                            }
                        }
                        jsonResult(McpJson.write(result))
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}

class ListLogFilesTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "list_log_files"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "type" to stringProperty(
                        "Log file category. Omit to list all categories.",
                        LogFileType.entries.map { it.name },
                    ),
                ),
            ),
        ).description(
            "List available log files across all categories (omit type) or one category. Each entry includes the " +
                    "access level get_log_file needs to read it. Listing itself is not access-restricted, so an " +
                    "empty result means no log files exist for the category (e.g. the server logs to the console " +
                    "in dev), not that access was denied."
        ).annotations(readOnly("List log files")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val typeStr = request.arguments()["type"] as? String
                val requestedType = typeStr?.let { runCatching { LogFileType.valueOf(it) }.getOrNull() }
                val types = requestedType?.let { listOf(it) } ?: LogFileType.entries
                ctx.withConnection { conn ->
                    val files = types.flatMap { type ->
                        conn.getLogFileDescriptors(type).map { f ->
                            mapOf(
                                "fileName" to f.fileName,
                                "type" to type.name,
                                "description" to f.shortDescription,
                                "readAccess" to type.minimumAccessLevel.name,
                            )
                        }
                    }
                    val result = buildMap<String, Any?> {
                        put("files", files)
                        if (files.isEmpty()) {
                            put(
                                "note",
                                "No log files found. Listing is not access-restricted, so this means no file logs " +
                                        "exist for this category (the server may be logging to the console, common " +
                                        "in dev), not that access was denied.",
                            )
                        }
                    }
                    jsonResult(McpJson.write(result))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}

class GetLogFileTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "get_log_file"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "fileName" to stringProperty("Log file name (from list_log_files)."),
                    "maxLines" to integerProperty("Maximum number of trailing lines to return. Default: 200."),
                ),
                listOf("fileName"),
            ),
        ).description(
            "Read the tail of a log file listed by list_log_files. Access depends on the log type: the event " +
                    "log needs viewer access, the connection log needs profiler access, and the server log needs " +
                    "admin access."
        ).annotations(readOnly("Get log file")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val fileName = args["fileName"] as String
                val maxLines = (args["maxLines"] as? Number)?.toInt() ?: 200
                ctx.withConnection { conn ->
                    // Only allow file names the user is actually allowed to list, which also blocks path traversal.
                    val allowed = LogFileType.entries
                        .flatMap { runCatching { conn.getLogFileDescriptors(it) }.getOrDefault(emptyList()) }
                        .map { it.fileName }
                        .toSet()
                    if (fileName !in allowed) {
                        errorResult("Unknown or inaccessible log file: $fileName")
                    } else {
                        val logFile = conn.getLogFile(fileName)
                        try {
                            val lines = logFile.componentDelta().lines
                            textResult(lines.takeLast(maxLines).joinToString("\n"))
                        } finally {
                            logFile.close()
                        }
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}

class GetSnapshotFileTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "get_snapshot_file"
        private const val MAX_INLINE_TEXT_BYTES = 256L * 1024L
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf("id" to integerProperty("Snapshot file id (from list_snapshot_files).")),
                listOf("id"),
            ),
        ).description(
            "Retrieve a captured diagnostic artifact by id (from list_snapshot_files). Thread dumps are " +
                    "returned as text. Binary artifacts (memory snapshots, CPU snapshots, JFR recordings) return " +
                    "metadata with a ready-to-use downloadUrl and downloadCommand. The download link carries a " +
                    "single-use token and needs no extra authentication; run the downloadCommand to save the file."
        ).annotations(readOnly("Get snapshot file")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val id = (request.arguments()["id"] as? Number)?.toLong()
                if (id == null) {
                    errorResult("Missing snapshot file id")
                } else {
                    ctx.withConnection { conn ->
                        val file = conn.getSnapshotFile(id)
                        if (file == null) {
                            errorResult("Snapshot file not found: $id")
                        } else if (file.type == SnapshotFileType.THREAD_DUMP && file.file.length() <= MAX_INLINE_TEXT_BYTES) {
                            textResult(file.file.readText())
                        } else {
                            jsonResult(McpJson.write(binaryArtifactMetadata(id, file)))
                        }
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun binaryArtifactMetadata(id: Long, file: com.jvmguard.data.file.SnapshotFile): Map<String, Any?> {
        val meta = linkedMapOf<String, Any?>(
            "id" to id,
            "vm" to file.vm.hierarchyPath,
            "type" to file.type.name,
            "name" to file.targetFileName,
            "sizeBytes" to file.file.length(),
        )
        val base = ctx.currentBaseUrl()
        if (base != null) {
            val url = "$base/mcp-artifacts/$id?dl=${ctx.createDownloadToken(id)}"
            meta["downloadUrl"] = url
            meta["downloadCommand"] = "curl -fSL -o \"${file.targetFileName}\" \"$url\""
            meta["note"] = "The download link is single-use and expires in 5 minutes. Run downloadCommand to save it."
        } else {
            meta["downloadPath"] = "/mcp-artifacts/$id"
            meta["note"] = "Download with an HTTP GET to downloadPath, relative to the MCP server base URL, " +
                    "using the Authorization: Bearer <API_KEY> header."
        }
        return meta
    }
}

class GetInboxTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "get_inbox"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(emptyMap()),
        ).description(
            "List trigger-generated inbox items (notifications about fired triggers, completed recordings, etc.). " +
                    "status is \"SNAPSHOT_READY\" when the item has an artifactId (fetch it with get_snapshot_file) " +
                    "or \"MESSAGE\" for a plain notification, whose 'message' field carries the detail."
        ).annotations(readOnly("Get inbox")).build()
        return SyncToolSpecification(tool) { _, _ ->
            try {
                ctx.withConnection { conn ->
                    val items = conn.inboxItems.map { item ->
                        val artifactId = item.snapshotFileId
                        buildMap<String, Any?> {
                            put("id", item.id)
                            put("date", item.date.toString())
                            put("status", if (artifactId != null) "SNAPSHOT_READY" else "MESSAGE")
                            put("name", item.name)
                            if (item.message.isNotBlank()) {
                                put("message", item.message)
                            }
                            artifactId?.let { put("artifactId", it) }
                            item.snapshotFileType?.let { put("snapshotType", it.name) }
                            // Pool members have a trailing slash which needs to be normalized.
                            put("vm", item.vm?.hierarchyPath?.trimEnd('/'))
                        }
                    }
                    jsonResult(McpJson.write(items))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}
