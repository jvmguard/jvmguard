package com.jvmguard.data.config.triggers.actions

import com.jvmguard.agent.config.base.ConfigDoc

abstract class ArtifactAction : TriggerAction {

    @field:ConfigDoc("Display name given to the produced artifact (snapshot/dump).")
    var artifactName: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

    @field:ConfigDoc("If true, an inbox item is created linking the artifact.")
    private var createInboxItem: Boolean = false

    var isCreateInboxItem: Boolean
        get() = createInboxItem
        set(value) {
            val old = createInboxItem
            createInboxItem = value
            fireChanged(old, value)
        }

    protected constructor() {
        artifactName = createDefaultArtifactName()
    }

    protected constructor(artifactName: String, createInboxItem: Boolean) {
        this.artifactName = artifactName
        this.createInboxItem = createInboxItem
    }

    protected abstract fun createDefaultArtifactName(): String

    override val parameterDescription: String?
        get() = null
}
