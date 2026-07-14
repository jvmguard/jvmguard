package dev.jvmguard.ui

import com.vaadin.browserless.BrowserlessTest
import com.vaadin.browserless.ComponentQuery
import com.vaadin.browserless.ViewPackages
import com.vaadin.browserless.locator.Locators
import com.vaadin.flow.component.Component

@ViewPackages(packages = ["dev.jvmguard.ui"])
abstract class JvmGuardBrowserlessTest : BrowserlessTest(), Locators {

    inline fun <reified T : Component> find(): ComponentQuery<T> = find(T::class.java)

    inline fun <reified T : Component> find(scope: Component): ComponentQuery<T> = find(T::class.java, scope)
}
