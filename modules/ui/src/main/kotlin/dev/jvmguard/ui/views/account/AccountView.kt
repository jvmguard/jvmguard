package dev.jvmguard.ui.views.account

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@PermitAll
@Route("account")
class AccountView : Div(), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        event.forwardTo(AccountProfileView::class.java)
    }
}
