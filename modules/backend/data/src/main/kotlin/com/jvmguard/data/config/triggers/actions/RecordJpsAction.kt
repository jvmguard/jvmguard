package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.data.config.triggers.TimeUnit

open class RecordJpsAction : RecordArtifactAction {

    var subsystems: Set<String> = JProfilerSubsystem.DEFAULT_IDS
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var heapDump: Boolean = false
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var heapDumpFullGc: Boolean = true
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var mbeanSnapshot: Boolean = false
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

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
