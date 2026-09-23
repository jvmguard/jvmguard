package dev.jvmguard.ui.server

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.communication.RpcInvocationEndedEvent
import com.vaadin.flow.server.communication.RpcInvocationFailedEvent
import com.vaadin.flow.server.communication.RpcInvocationStartedEvent
import com.vaadin.flow.server.data.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object UiPerformanceMonitor {

    fun register(service: VaadinService) {
        with(service.eventBus) {
            addListener(RpcInvocationStartedEvent::class.java) {
                starts[RpcKey(it.ui, it.type, it.nodeId, it.name)] = System.nanoTime()
            }
            addListener(RpcInvocationEndedEvent::class.java) { event ->
                end(RpcKey(event.ui, event.type, event.nodeId, event.name), SLOW_RPC_THRESHOLD_MS) {
                    "RPC ${event.type} '${event.name}'"
                }
            }
            addListener(RpcInvocationFailedEvent::class.java) {
                starts.remove(RpcKey(it.ui, it.type, it.nodeId, it.name))
            }

            addListener(DataFetchStartedEvent::class.java) {
                starts[FetchKey(it.ui, it.component.orElse(null), it.offset, it.limit)] = System.nanoTime()
            }
            addListener(DataFetchEndedEvent::class.java) { event ->
                end(FetchKey(event.ui, event.component.orElse(null), event.offset, event.limit), SLOW_QUERY_THRESHOLD_MS) {
                    "data fetch ${event.offset}..${event.offset + event.limit} (${event.rowsReturned} rows)" +
                            " for ${describe(event.component.orElse(null))}"
                }
            }
            addListener(DataFetchFailedEvent::class.java) {
                starts.remove(FetchKey(it.ui, it.component.orElse(null), it.offset, it.limit))
            }

            addListener(DataCountStartedEvent::class.java) {
                starts[CountKey(it.ui, it.component.orElse(null))] = System.nanoTime()
            }
            addListener(DataCountEndedEvent::class.java) { event ->
                end(CountKey(event.ui, event.component.orElse(null)), SLOW_QUERY_THRESHOLD_MS) {
                    "count query (${event.count} rows) for ${describe(event.component.orElse(null))}"
                }
            }
            addListener(DataCountFailedEvent::class.java) {
                starts.remove(CountKey(it.ui, it.component.orElse(null)))
            }
        }
        service.addUIInitListener { event -> event.ui.addDetachListener { evict(event.ui) } }
    }

    private fun end(key: UiKey, slowThresholdMs: Long, description: () -> String) {
        val started = starts.remove(key) ?: return
        val millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
        if (millis >= slowThresholdMs) {
            LOGGER.warn("Slow UI server-side processing: {} took {} ms", description(), millis)
        } else if (LOGGER.isTraceEnabled) {
            LOGGER.trace("{} took {} ms", description(), millis)
        }
    }

    private fun evict(ui: UI) {
        starts.keys.removeIf { it.ui === ui }
    }

    private fun describe(component: Component?): String {
        if (component == null) {
            return "an unknown component"
        }
        val chain = generateSequence(component) { it.parent.orElse(null) }.toList()
        val view = chain.firstOrNull { it.javaClass.isAnnotationPresent(Route::class.java) } ?: chain.last()
        return "${component.javaClass.simpleName} in ${view.javaClass.simpleName}"
    }

    private sealed interface UiKey {
        val ui: UI
    }

    private data class RpcKey(override val ui: UI, val type: String, val nodeId: Int, val name: String) : UiKey

    private data class FetchKey(override val ui: UI, val component: Component?, val offset: Int, val limit: Int) : UiKey

    private data class CountKey(override val ui: UI, val component: Component?) : UiKey

    private val starts = ConcurrentHashMap<UiKey, Long>()

    private const val SLOW_RPC_THRESHOLD_MS = 1_000L
    private const val SLOW_QUERY_THRESHOLD_MS = 500L

    private val LOGGER = LoggerFactory.getLogger(UiPerformanceMonitor::class.java)
}
