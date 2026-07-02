package com.jvmguard.data.config.triggers.actions

import com.jvmguard.data.config.triggers.TimeUnit

abstract class RecordArtifactAction : ArtifactAction {

    var time: Int = 1
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    var timeUnit: TimeUnit = TimeUnit.MINUTES
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    protected constructor()

    protected constructor(artifactName: String, createInboxItem: Boolean, time: Int, timeUnit: TimeUnit) :
            super(artifactName, createInboxItem) {
        this.time = time
        this.timeUnit = timeUnit
    }

    override val parameterDescription: String
        get() = "$time ${timeUnit.getName(time)}"
}
