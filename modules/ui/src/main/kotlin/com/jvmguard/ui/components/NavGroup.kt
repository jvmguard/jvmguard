package com.jvmguard.ui.components

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

/**
 * A tight cluster of related buttons (e.g. paging/zoom) that reads as one connected button bar,
 * rather than being spread by a toolbar's wider control gap. The small inter-button gap is in
 * `styles.css` (`.jvmguard-navgroup`).
 */
class NavGroup(vararg buttons: Component) : HorizontalLayout(*buttons) {
    init {
        addClassName("jvmguard-navgroup")
        isPadding = false
        isSpacing = false
        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
    }
}
