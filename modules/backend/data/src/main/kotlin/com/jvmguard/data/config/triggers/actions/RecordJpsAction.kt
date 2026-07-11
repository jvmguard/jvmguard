package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.ConfigDoc
import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.data.config.triggers.TimeUnit

@ConfigDoc("Records a JProfiler snapshot (.jps).")
open class RecordJpsAction : RecordArtifactAction {

    @field:ConfigDoc("JProfiler recording subsystem ids to enable (e.g. cpu, jdbc, jpa, http-server).")
    var subsystems: Set<String> = JProfilerSubsystem.DEFAULT_IDS
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @field:ConfigDoc("Also capture a heap dump in the snapshot.")
    var heapDump: Boolean = false
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @field:ConfigDoc("Perform a full GC before the heap dump.")
    var heapDumpFullGc: Boolean = true
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @field:ConfigDoc("Include an MBean snapshot.")
    var mbeanSnapshot: Boolean = false
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @field:ConfigDoc("Include a monitor (lock) dump.")
    var monitorDump: Boolean = false
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @DefaultConstructor
    constructor()

    constructor(artifactName: String, createInboxItem: Boolean, time: Int, timeUnit: TimeUnit) :
            super(artifactName, createInboxItem, time, timeUnit)

    override val actionType: ActionType
        get() = ActionType.RECORD_JPS

    override fun createDefaultArtifactName(): String = "JProfiler snapshot"
}
