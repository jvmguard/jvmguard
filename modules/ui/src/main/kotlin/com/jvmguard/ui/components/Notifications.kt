package com.jvmguard.ui.components

import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.notification.Notification

object Notifications {

    private const val OPEN_NOTIFICATIONS = "openNotifications"

    fun show(text: String): Notification {
        val notification = Notification.show(text)
        UI.getCurrent()?.let { ui ->
            val open = open(ui)
            open.add(notification)
            notification.addOpenedChangeListener { if (!it.isOpened) open.remove(notification) }
        }
        return notification
    }

    fun closeAll() {
        val ui = UI.getCurrent() ?: return
        open(ui).toList().forEach { it.close() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun open(ui: UI): MutableSet<Notification> =
        (ComponentUtil.getData(ui, OPEN_NOTIFICATIONS) as? MutableSet<Notification>)
            ?: mutableSetOf<Notification>().also { ComponentUtil.setData(ui, OPEN_NOTIFICATIONS, it) }
}
