package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.agent.config.base.ConfigDoc
import dev.jvmguard.data.config.triggers.TimeUnit

sealed class RecordArtifactAction : ArtifactAction {

    @field:ConfigDoc("Recording duration.")
    var time: Int = 1
        set(value) { field = changed(field, value) }

    @field:ConfigDoc("Unit of the recording duration.")
    var timeUnit: TimeUnit = TimeUnit.MINUTES
        set(value) { field = changed(field, value) }

    protected constructor()

    protected constructor(artifactName: String, createInboxItem: Boolean, time: Int, timeUnit: TimeUnit) :
            super(artifactName, createInboxItem) {
        this.time = time
        this.timeUnit = timeUnit
    }

    override val parameterDescription: String
        get() = "$time ${timeUnit.getName(time)}"
}
