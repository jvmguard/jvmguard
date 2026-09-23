package dev.jvmguard.ui.server

import dev.jvmguard.common.notification.ModificationType
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.shared.Registration

fun interface ModificationListener {
    fun modifyNotified(modificationTypes: Set<ModificationType>)
}

fun <T> T.registerModificationListener(session: UserSession) where T : Component, T : ModificationListener {
    if (ComponentUtil.getData(this, ModificationListenerWiring::class.java) != null) {
        return
    }
    ComponentUtil.setData(this, ModificationListenerWiring::class.java, ModificationListenerWiring)
    whenAttached {
        session.addModificationListener(this)
        Registration { session.removeModificationListener(this) }
    }
}

private object ModificationListenerWiring
