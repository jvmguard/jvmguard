package com.jvmguard.mcp.tool

import com.jvmguard.data.config.triggers.TimeUnit
import com.jvmguard.data.config.triggers.actions.RecordJfrAction
import com.jvmguard.data.config.triggers.actions.RecordJpsAction
import com.jvmguard.mcp.McpToolContext
import io.modelcontextprotocol.spec.McpSchema.Tool
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification

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
                    "Requires profiler access. The dump will appear in list_snapshot_files when ready."
        ).annotations(action("Heap dump")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val vmPath = request.arguments()["vm"] as String
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVm(conn, vmPath)
                    conn.heapDump(vm)
                    textResult("Heap dump triggered for $vmPath")
                }
            } catch (e: Exception) {
                handleError(e)
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
            "Trigger a thread dump on the specified VM. Requires profiler access. " +
                    "The dump will appear in list_snapshot_files when ready."
        ).annotations(action("Thread dump")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val vmPath = request.arguments()["vm"] as String
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVm(conn, vmPath)
                    conn.threadDump(vm)
                    textResult("Thread dump triggered for $vmPath")
                }
            } catch (e: Exception) {
                handleError(e)
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
            try {
                val vmPath = request.arguments()["vm"] as String
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVm(conn, vmPath)
                    conn.runGC(vm)
                    textResult("GC requested for $vmPath")
                }
            } catch (e: Exception) {
                handleError(e)
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
            "Start a JFR (Java Flight Recorder) recording on the specified VM. " +
                    "Requires profiler access. The recording will appear in list_snapshot_files when complete."
        ).annotations(action("Record JFR")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
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
                    conn.recordJfr(vm, action)
                    textResult("JFR recording (${durationSeconds}s) started for $vmPath")
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}

class RecordJpsTool(ctx: McpToolContext) : McpTool(ctx) {

    companion object {
        const val NAME = "record_jps"
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
            "Start a JProfiler snapshot recording on the specified VM. " +
                    "Requires profiler access. The snapshot will appear in list_snapshot_files when complete."
        ).annotations(action("Record JProfiler snapshot")).build()
        return SyncToolSpecification(tool) { _, request ->
            try {
                val args = request.arguments()
                val vmPath = args["vm"] as String
                val durationSeconds = (args["durationSeconds"] as? Int) ?: 60
                ctx.withConnection { conn ->
                    val vm = VmResolver.resolveVm(conn, vmPath)
                    val action = RecordJpsAction().apply {
                        time = durationSeconds
                        timeUnit = TimeUnit.SECONDS
                        isCreateInboxItem = true
                        artifactName = vm.rawName
                    }
                    conn.recordJps(vm, action)
                    textResult("JProfiler snapshot recording (${durationSeconds}s) started for $vmPath")
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}
