package dev.jvmguard.ui.server

import dev.jvmguard.ui.shell.CachedView
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.HasElement
import com.vaadin.flow.component.UI
import com.vaadin.flow.di.DefaultInstantiator
import com.vaadin.flow.di.Instantiator
import com.vaadin.flow.di.InstantiatorFactory
import com.vaadin.flow.router.NavigationEvent
import com.vaadin.flow.server.VaadinService

/**
 * Auto-discovered by Vaadin from the classpath
 */
@Suppress("unused")
class KeepAliveInstantiatorFactory : InstantiatorFactory {
    override fun createInstantitor(service: VaadinService): Instantiator = KeepAliveInstantiator(service)
}

class KeepAliveInstantiator(service: VaadinService) : DefaultInstantiator(service) {

    override fun <T : HasElement> createRouteTarget(routeTargetType: Class<T>, event: NavigationEvent): T {
        if (CachedView::class.java.isAssignableFrom(routeTargetType)) {
            val cache = cache(event.ui)
            @Suppress("UNCHECKED_CAST")
            return cache.getOrPut(routeTargetType) { super.createRouteTarget(routeTargetType, event) } as T
        }
        return super.createRouteTarget(routeTargetType, event)
    }

    companion object {
        private const val CACHE_KEY = "keepAliveViewCache"

        @Suppress("UNCHECKED_CAST")
        private fun cache(ui: UI): MutableMap<Class<*>, HasElement> =
            (ComponentUtil.getData(ui, CACHE_KEY) as? MutableMap<Class<*>, HasElement>)
                ?: HashMap<Class<*>, HasElement>().also { ComponentUtil.setData(ui, CACHE_KEY, it) }

        fun instances(ui: UI): Collection<HasElement> = cache(ui).values.toList()

        fun evict(ui: UI, predicate: (HasElement) -> Boolean) {
            cache(ui).values.removeAll { predicate(it) }
        }

        fun clear(ui: UI) = ComponentUtil.setData(ui, CACHE_KEY, null)
    }
}
