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
                    "limit" to integerProperty("Maximum number of files to return. Default: 200."),
                ),
            ),
        ).description(
            "List captured diagnostic artifacts (memory snapshots, CPU snapshots, thread dumps, JFR recordings). " +
                    "These are produced by heap_dump, thread_dump, record_jfr, record_jps, or by triggers. " +
                    "Retrieve one with get_snapshot_file."
        ).annotations(readOnly("List snapshot files")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as? String
                val typeStr = args["type"] as? String
                val limit = (args["limit"] as? Number)?.toInt() ?: 200
                val type = typeStr?.let { runCatching { SnapshotFileType.valueOf(it) }.getOrNull() }
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVmOrNull(conn, vmPath)
                    val files = conn.getSnapshotFiles(type, vm).take(limit).map { f ->
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
                    "Use the returned names in get_mbean_data to inspect attributes."
        ).annotations(readOnly("List MBeans")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as String
                val includePlatform = (args["includePlatform"] as? Boolean) ?: true
                val limit = (args["limit"] as? Number)?.toInt() ?: 500
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVm(conn, vmPath)
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
                    "vm" to stringProperty("VM hierarchy path."),
                    "name" to stringProperty("MBean object name (from list_mbeans), e.g. \"java.lang:type=Memory\"."),
                    "fetchValues" to booleanProperty("If true, fetch attribute values. Default: true."),
                ),
                listOf("vm", "name"),
            ),
        ).description(
            "Retrieve MBean attributes and structure for a specific MBean. " +
                    "Returns attribute names, types, and current values."
        ).annotations(readOnly("Get MBean data")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as String
                val name = args["name"] as String
                val fetchValues = (args["fetchValues"] as? Boolean) ?: true
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVm(conn, vmPath)
                    val data = conn.getMBeanData(vm, name, true, fetchValues)
                    if (data != null) {
                        jsonResult(McpJson.write(data))
                    } else {
                        errorResult("MBean not found: $name")
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
                        "Log file category.",
                        LogFileType.entries.map { it.name },
                    ),
                ),
            ),
        ).description(
            "List available log files (server log, event log, VM console logs, etc.). " +
                    "Read one with get_log_file."
        ).annotations(readOnly("List log files")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val typeStr = request.arguments()["type"] as? String
                val type = typeStr?.let { runCatching { LogFileType.valueOf(it) }.getOrNull() }
                    ?: LogFileType.SERVER
                ctx.withConnection { conn ->
                    val files = conn.getLogFileDescriptors(type).map { f ->
                        mapOf(
                            "fileName" to f.fileName,
                            "description" to f.shortDescription,
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
            "List trigger-generated inbox items (notifications about fired triggers, completed recordings, etc.)."
        ).annotations(readOnly("Get inbox")).build()
        return SyncToolSpecification(tool) { _, _ ->
            try {
                ctx.withConnection { conn ->
                    val items = conn.inboxItems.map { item ->
                        mapOf(
                            "id" to item.id,
                            "date" to item.date.toString(),
                            "name" to item.name,
                            "message" to item.message,
                            "vm" to item.vm?.hierarchyPath,
                        )
                    }
                    jsonResult(McpJson.write(items))
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}
