package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.data.config.triggers.TimeUnit

open class RecordJpsAction : RecordArtifactAction {

    @DefaultConstructor
    constructor()

    constructor(artifactName: String, createInboxItem: Boolean, time: Int, timeUnit: TimeUnit) :
            super(artifactName, createInboxItem, time, timeUnit)

    override val actionType: ActionType
        get() = ActionType.RECORD_JPS

    override fun createDefaultArtifactName(): String = "JProfiler snapshot"
}
