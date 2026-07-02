package com.jvmguard.ui.server

import com.jvmguard.common.notification.ModificationType
import com.vaadin.flow.component.Component

fun interface ModificationListener {
    fun modifyNotified(modificationTypes: Set<ModificationType>)
}

fun <T> T.registerModificationListener(session: UserSession) where T : Component, T : ModificationListener {
    session.addModificationListener(this)
    addDetachListener { session.removeModificationListener(this) }
}
