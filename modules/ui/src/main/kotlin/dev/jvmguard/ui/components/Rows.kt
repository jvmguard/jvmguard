package dev.jvmguard.ui.components

import dev.jvmguard.agent.config.VmType
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

const val GRID_ICON_SIZE = "1.4em"

fun vmTypeIcon(type: VmType): VaadinIcon = when (type) {
    VmType.POOL, VmType.POOLED -> VaadinIcon.CLUSTER
    VmType.GROUP -> VaadinIcon.FOLDER_O
    else -> VaadinIcon.SERVER
}

fun cellRow(vararg components: Component, gap: String = "0.4em"): HorizontalLayout =
    HorizontalLayout(*components).apply {
        defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        isSpacing = false
        isPadding = false
        style.set("gap", gap)
    }
