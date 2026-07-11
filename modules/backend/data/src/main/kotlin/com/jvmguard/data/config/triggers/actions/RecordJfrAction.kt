package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.ConfigDoc
import com.jvmguard.agent.config.base.DefaultConstructor
import com.jvmguard.data.config.triggers.TimeUnit

@ConfigDoc("Records a Java Flight Recorder (.jfr) recording.")
open class RecordJfrAction : RecordArtifactAction {

    @field:ConfigDoc("Use a predefined JFR profile or an explicit config file.")
    var configMode: JfrConfigMode = JfrConfigMode.PREDEFINED
    @field:ConfigDoc("Name of the predefined JFR profile when configMode is PREDEFINED. Valid values are default " +
        "and profile.")
    var profileName: String = JfrDefaultProfile.PROFILE.toString()
    @field:ConfigDoc("JFR settings/config-file content when configMode=CONFIG_FILE.")
    var settings: String = ""

    @DefaultConstructor
    constructor()

    constructor(artifactName: String, createInboxItem: Boolean, seconds: Int, timeUnit: TimeUnit) :
            super(artifactName, createInboxItem, seconds, timeUnit)

    override val actionType: ActionType
        get() = ActionType.RECORD_JFR

    override fun createDefaultArtifactName(): String = "JFR snapshot"
}
