package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.agent.config.base.ConfigDoc

@ConfigDoc("Captures a thread dump.")
open class ThreadDumpAction : ArtifactAction() {

    override val actionType: ActionType
        get() = ActionType.THREAD_DUMP

    override fun createDefaultArtifactName(): String = ARTIFACT_NAME

    companion object {
        const val ARTIFACT_NAME: String = "Thread dump"
    }
}
