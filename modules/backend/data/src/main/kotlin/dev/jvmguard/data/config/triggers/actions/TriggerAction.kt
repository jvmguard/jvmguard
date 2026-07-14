package dev.jvmguard.data.config.triggers.actions

import dev.jvmguard.data.base.PolymorphicJson
import dev.jvmguard.data.base.StoredConfig

@PolymorphicJson
sealed class TriggerAction : StoredConfig(), Cloneable {

    abstract val actionType: ActionType

    protected abstract val parameterDescription: String?

    open val description: String
        get() = parameterDescription?.let { "$actionType [$it]" } ?: actionType.toString()

    public override fun clone(): TriggerAction = super.clone() as TriggerAction
}
