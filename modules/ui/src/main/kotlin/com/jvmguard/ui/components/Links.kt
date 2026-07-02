package com.jvmguard.ui.components

import com.vaadin.flow.component.html.Span

fun textLink(text: String, onActivate: () -> Unit): Span = Span(text).apply {
    addClassName("jvmguard-vm-link")
    element.setAttribute("tabindex", "0")
    element.setAttribute("role", "link")
    addClickListener { onActivate() }
    element.addEventListener("keydown") { onActivate() }
        .setFilter("event.key === 'Enter' || event.key === ' '")
        .addEventData("event.preventDefault()")
}
