package com.jvmguard.ui.views.settings

import com.jvmguard.data.user.Roles
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed

@RolesAllowed(Roles.ADMIN)
@Route("settings")
class SettingsView : Div(), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        event.forwardTo(UsersView::class.java)
    }
}
