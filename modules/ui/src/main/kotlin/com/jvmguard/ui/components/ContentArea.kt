package com.jvmguard.ui.components

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout

/** Shows [content] stretched to fill the area, growing to take all space */
fun VerticalLayout.showFilling(content: Component) {
    if (content.parent.isEmpty) {
        removeAll()
        alignItems = FlexComponent.Alignment.STRETCH
        justifyContentMode = FlexComponent.JustifyContentMode.START
        add(content)
        setFlexGrow(1.0, content)
    }
}

/** Shows [content] centered in the area, replacing whatever was shown. */
fun VerticalLayout.showCentered(content: Component) {
    removeAll()
    alignItems = FlexComponent.Alignment.CENTER
    justifyContentMode = FlexComponent.JustifyContentMode.CENTER
    add(content)
}
