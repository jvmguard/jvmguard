package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.data.config.triggers.TimeUnit

open class RecordJfrAction : RecordArtifactAction {

    var configMode: JfrConfigMode = JfrConfigMode.PREDEFINED
    var profileName: String = JfrDefaultProfile.PROFILE.toString()
    var settings: String = ""

    @DefaultConstructor
    constructor()

    constructor(artifactName: String, createInboxItem: Boolean, seconds: Int, timeUnit: TimeUnit) :
            super(artifactName, createInboxItem, seconds, timeUnit)

    override val actionType: ActionType
        get() = ActionType.RECORD_JFR

    override fun createDefaultArtifactName(): String = "JFR snapshot"
}
