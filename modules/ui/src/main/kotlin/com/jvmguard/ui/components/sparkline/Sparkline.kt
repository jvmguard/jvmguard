package com.jvmguard.ui.components.sparkline

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.dependency.JsModule

@Suppress("unused")
@Tag("jvmguard-sparkline")
@JsModule("./sparkline.ts")
class Sparkline : Component() {

    fun setState(state: SparklineState) {
        element.setPropertyBean("state", state)
    }
}
