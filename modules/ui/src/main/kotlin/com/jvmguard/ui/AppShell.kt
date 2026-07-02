package com.jvmguard.ui

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.ColorScheme
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.theme.aura.Aura

@ColorScheme(ColorScheme.Value.SYSTEM)
@Push
class AppShell : AppShellConfigurator {

    override fun configurePage(settings: AppShellSettings) {
        // Add the stylesheets as plain links rather than via
        // @StyleSheet because @StyleSheet runs a server-side content-hash that calls ServletContext.getResource
        // on the relative URL. addLink emits the same links resolved against the base href
        settings.addLink(Aura.STYLESHEET, mapOf("rel" to "stylesheet"))
        settings.addLink("styles.css", mapOf("rel" to "stylesheet"))
        settings.addFavIcon("icon", "icons/favicon.svg", "48x48")
    }
}
