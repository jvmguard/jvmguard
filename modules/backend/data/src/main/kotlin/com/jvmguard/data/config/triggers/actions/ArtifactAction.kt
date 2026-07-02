package com.jvmguard.data.config.triggers.actions

abstract class ArtifactAction : TriggerAction {

    var artifactName: String = ""
        set(value) {
            val old = field
            field = value
            fireChanged(old, value)
        }

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
