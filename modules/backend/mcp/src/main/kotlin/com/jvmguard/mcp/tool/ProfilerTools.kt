package com.jvmguard.mcp.tool

import com.jvmguard.data.config.triggers.TimeUnit
import com.jvmguard.data.config.triggers.actions.JProfilerSubsystem
import com.jvmguard.data.config.triggers.actions.RecordJfrAction
import com.jvmguard.data.config.triggers.actions.RecordJpsAction
import com.jvmguard.data.file.SnapshotFileType
import com.jvmguard.mcp.McpJson
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.spec.McpSchema.Tool
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification

internal fun captureAck(
    status: String,
    vmPath: String,
    type: SnapshotFileType,
    triggeredAt: Long,
    estimatedSeconds: Int,
    durationSeconds: Int? = null,
    extras: Map<String, Any?> = emptyMap(),
): Map<String, Any?> = buildMap {
    put("status", status)
    put("vm", vmPath)
    put("type", type.name)
    durationSeconds?.let { put("durationSeconds", it) }
    putAll(extras)
    put("estimatedSeconds", estimatedSeconds)
    put("triggeredAt", triggeredAt)
    put("estimatedReadyAt", triggeredAt + estimatedSeconds * 1000L)
    put(
        "nextStep",
        "After estimatedReadyAt, call list_snapshot_files with type=\"${type.name}\", vm=\"$vmPath\", " +
                "since=$triggeredAt; the newest entry is this capture. Fetch it with get_snapshot_file.",
    )
}

class HeapDumpTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "heap_dump"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf("vm" to stringProperty("VM hierarchy path (from list_vms).")),
                listOf("vm"),
            ),
        ).description(
            "Trigger a memory snapshot (heap dump) on the specified VM. " +
                    "Requires profiler access. Returns a readiness estimate and how to locate the artifact; " +
                    "the dump appears in list_snapshot_files when ready."
        ).annotations(action("Heap dump")).build()
        return SyncToolSpecification(tool) { _, request ->
            val vmPath = request.arguments()["vm"] as String
            ctx.withConnection { conn ->
                val vm = VmResolver.resolveVm(conn, vmPath)
                val triggeredAt = System.currentTimeMillis()
                conn.heapDump(vm)
                jsonResult(McpJson.write(captureAck("capturing", vmPath, SnapshotFileType.HPZ, triggeredAt, estimatedSeconds = 5)))
            }
        }
    }
}

class ThreadDumpTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "thread_dump"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf("vm" to stringProperty("VM hierarchy path (from list_vms).")),
                listOf("vm"),
            ),
        ).description(
            "Trigger a thread dump on the specified VM. Requires profiler access. Returns a readiness estimate " +
                    "and how to locate the artifact; the dump appears in list_snapshot_files (get_snapshot_file " +
                    "returns it as text)."
        ).annotations(action("Thread dump")).build()
        return SyncToolSpecification(tool) { _, request ->
            val vmPath = request.arguments()["vm"] as String
            ctx.withConnection { conn ->
                val vm = VmResolver.resolveVm(conn, vmPath)
                val triggeredAt = System.currentTimeMillis()
                conn.threadDump(vm)
                jsonResult(McpJson.write(captureAck("capturing", vmPath, SnapshotFileType.THREAD_DUMP, triggeredAt, estimatedSeconds = 2)))
            }
        }
    }
}

class RunGcTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "run_gc"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf("vm" to stringProperty("VM hierarchy path (from list_vms).")),
                listOf("vm"),
            ),
        ).description(
            "Request a garbage collection on the specified VM. Requires profiler access."
        ).annotations(action("Run GC")).build()
        return SyncToolSpecification(tool) { _, request ->
            val vmPath = request.arguments()["vm"] as String
            ctx.withConnection { conn ->
                val vm = VmResolver.resolveVm(conn, vmPath)
                conn.runGC(vm)
                textResult("GC requested for $vmPath")
            }
        }
    }
}

class RecordJfrTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "record_jfr"
    }

    override fun createSpecification(): SyncToolSpecification {
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path (from list_vms)."),
                    "durationSeconds" to integerProperty(
                        "Recording duration in seconds. Default: 60."
                    ),
                ),
                listOf("vm"),
            ),
        ).description(
            "Start a JFR (Java Flight Recorder) recording on the specified VM. Requires profiler access. " +
                    "Returns a readiness estimate and how to locate the artifact; the recording appears in " +
                    "list_snapshot_files when complete."
        ).annotations(action("Record JFR")).build()
        return SyncToolSpecification(tool) { _, request ->
            val args = request.arguments()
            val vmPath = args["vm"] as String
            val durationSeconds = (args["durationSeconds"] as? Int) ?: 60
            ctx.withConnection { conn ->
                val vm = VmResolver.resolveVm(conn, vmPath)
                val action = RecordJfrAction().apply {
                    time = durationSeconds
                    timeUnit = TimeUnit.SECONDS
                    isCreateInboxItem = true
                    artifactName = vm.rawName
                }
                val triggeredAt = System.currentTimeMillis()
                conn.recordJfr(vm, action)
                jsonResult(McpJson.write(captureAck("recording", vmPath, SnapshotFileType.JFR, triggeredAt, durationSeconds + 5, durationSeconds)))
            }
        }
    }
}

class RecordJpsTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "record_jps"
    }

    override fun createSpecification(): SyncToolSpecification {
        val defaultIds = JProfilerSubsystem.entries.filter { it.id in JProfilerSubsystem.DEFAULT_IDS }.map { it.id }
        val tool = Tool.builder(
            NAME,
            objectSchema(
                mapOf(
                    "vm" to stringProperty("VM hierarchy path (from list_vms)."),
                    "durationSeconds" to integerProperty(
                        "Recording duration in seconds. Default: 60."
                    ),
                    "subsystems" to stringArrayProperty(
                        "Which JProfiler probes/subsystems to record. Omit to use the default set " +
                                "(${defaultIds.joinToString(", ")}). Pass an explicit list to add probes such as " +
                                "\"socket\", \"file\", or \"allocation\", or to narrow the recording.",
                        JProfilerSubsystem.entries.map { it.id },
                    ),
                    "heapDump" to booleanProperty("Also capture a heap dump into the snapshot. Default: false."),
                    "heapDumpFullGc" to booleanProperty(
                        "When heapDump is true, run a full GC first so only reachable objects are dumped. Default: true."
                    ),
                    "mbeanSnapshot" to booleanProperty("Also capture the MBean tree into the snapshot. Default: false."),
                    "monitorDump" to booleanProperty("Also capture a monitor (lock) dump into the snapshot. Default: false."),
                ),
                listOf("vm"),
            ),
        ).description(
            "Start a JProfiler snapshot recording on the specified VM. Requires profiler access. " +
                    "'subsystems' selects which probes are recorded (default: " +
                    "${defaultIds.joinToString(", ")}); heapDump/mbeanSnapshot/monitorDump add extra captures to " +
                    "the snapshot. The response echoes the effective settings. " +
                    "Returns a readiness estimate and how to locate the artifact; the snapshot appears in " +
                    "list_snapshot_files when complete."
        ).annotations(action("Record JProfiler snapshot")).build()
        return SyncToolSpecification(tool) { _, request ->
            val args = request.arguments()
            val vmPath = args["vm"] as String
            val durationSeconds = (args["durationSeconds"] as? Int) ?: 60
            val requestedSubsystems = (args["subsystems"] as? List<*>)?.map { it.toString() }
            val unknown = requestedSubsystems?.filter { JProfilerSubsystem.fromId(it) == null }.orEmpty()
            if (unknown.isNotEmpty()) {
                errorResult(
                    "Unknown subsystem(s): ${unknown.joinToString(", ")}. " +
                            "Valid ids: ${JProfilerSubsystem.entries.joinToString(", ") { it.id }}."
                )
            } else {
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVm(conn, vmPath)
                    val action = RecordJpsAction().apply {
                        time = durationSeconds
                        timeUnit = TimeUnit.SECONDS
                        isCreateInboxItem = true
                        artifactName = vm.rawName
                        requestedSubsystems?.let { subsystems = it.toSet() }
                        (args["heapDump"] as? Boolean)?.let { heapDump = it }
                        (args["heapDumpFullGc"] as? Boolean)?.let { heapDumpFullGc = it }
                        (args["mbeanSnapshot"] as? Boolean)?.let { mbeanSnapshot = it }
                        (args["monitorDump"] as? Boolean)?.let { monitorDump = it }
                    }
                    val triggeredAt = System.currentTimeMillis()
                    conn.recordJps(vm, action)
                    jsonResult(
                        McpJson.write(
                            captureAck(
                                "recording", vmPath, SnapshotFileType.JPS, triggeredAt, durationSeconds + 5,
                                durationSeconds,
                                extras = mapOf(
                                    "subsystems" to action.subsystems.toList(),
                                    "heapDump" to action.heapDump,
                                    "heapDumpFullGc" to action.heapDumpFullGc,
                                    "mbeanSnapshot" to action.mbeanSnapshot,
                                    "monitorDump" to action.monitorDump,
                                ),
                            )
                        )
                    )
                }
            }
        }
    }
}
