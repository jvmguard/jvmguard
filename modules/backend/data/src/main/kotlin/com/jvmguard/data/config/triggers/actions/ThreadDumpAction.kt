package com.jvmguard.data.config.triggers.actions

open class ThreadDumpAction : ArtifactAction() {

    override val actionType: ActionType
        get() = ActionType.THREAD_DUMP

    override fun createDefaultArtifactName(): String = ARTIFACT_NAME

    companion object {
        const val ARTIFACT_NAME: String = "Thread dump"
    }
}
