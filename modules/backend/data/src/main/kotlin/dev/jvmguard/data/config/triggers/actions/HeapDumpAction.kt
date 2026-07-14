package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.agent.config.base.ConfigDoc

@ConfigDoc("Captures a heap (memory) snapshot.")
open class HeapDumpAction : ArtifactAction() {

    override val actionType: ActionType
        get() = ActionType.HEAP_DUMP

    override fun createDefaultArtifactName(): String = ARTIFACT_NAME

    companion object {
        const val ARTIFACT_NAME: String = "Memory snapshot"
    }
}
