package com.jvmguard.ui.components.recording

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs

/**
 * A tabbed container for dialog wizards. Unlike [com.vaadin.flow.component.tabs.TabSheet], every panel
 * stays laid out, so all fields are measured once when the dialog opens.
 */
class WizardTabs : VerticalLayout() {

    private val tabs = Tabs().apply { setWidthFull() }
    private val host = Div().apply {
        setWidthFull()
        style.set("position", "relative").set("height", CONTENT_HEIGHT)
    }
    private val panels = mutableListOf<Div>()

    init {
        isPadding = false
        isSpacing = false
        setWidthFull()
        tabs.addSelectedChangeListener { showSelected() }
        add(tabs, host)
    }

    fun addTab(title: String, content: Component): WizardTabs {
        tabs.add(Tab(title))
        val panel = Div(content).apply {
            style.set("position", "absolute")
                .set("inset", "0")
                .set("overflow-y", "auto")
                .set("overflow-x", "hidden")
                .set("box-sizing", "border-box")
                .set("padding", "var(--vaadin-padding-m, 1rem)")
        }
        panels.add(panel)
        host.add(panel)
        showSelected()
        return this
    }

    private fun showSelected() {
        val index = tabs.selectedIndex
        panels.forEachIndexed { i, panel ->
            panel.style.set("visibility", if (i == index) "visible" else "hidden")
        }
    }

    companion object {
        private const val CONTENT_HEIGHT = "26rem"
    }
}
