package com.jvmguard.data.config.triggers.actions

open class HeapDumpAction : ArtifactAction() {

    override val actionType: ActionType
        get() = ActionType.HEAP_DUMP

    override fun createDefaultArtifactName(): String = ARTIFACT_NAME

    companion object {
        const val ARTIFACT_NAME: String = "Memory snapshot"
    }
}
