package dev.jvmguard.ui.components.echart

import com.vaadin.flow.component.*
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.dependency.NpmPackage
import com.vaadin.flow.shared.Registration

/**
 * A thin Flow wrapper around an Apache ECharts instance. The chart is driven by a
 * TelemetryChartModel. The ECharts option is built on the client in `jvmguard-echart.ts`. The
 * element self-detects the effective light/dark color scheme and re-themes on toggle.
 */
@Suppress("unused")
@Tag("jvmguard-echart")
@NpmPackage(value = "echarts", version = "6.1.0")
@JsModule("./echart.ts")
class EChart : Component(), HasSize {

    /** Epoch millis under the pointer at the last right-click, for context-menu actions. */
    var lastContextTime: Long = 0L
        private set

    /** The y-axis bounds last rendered (nice round numbers), for "Freeze Y-axis". NaN until known. */
    var lastYMin: Double = Double.NaN
        private set
    var lastYMax: Double = Double.NaN
        private set

    init {
        element.style.set("display", "block")
        addListener(ContextTimeEvent::class.java) { lastContextTime = it.time }
        addListener(YExtentEvent::class.java) {
            lastYMin = it.min
            lastYMax = it.max
        }
    }

    fun setModel(model: TelemetryChartModel) {
        element.setPropertyBean("model", model)
    }

    fun addZoomInListener(listener: (Long) -> Unit): Registration =
        addListener(ZoomInEvent::class.java) { listener(it.time) }

    fun addPointClickListener(listener: (PointClickEvent) -> Unit): Registration =
        addListener(PointClickEvent::class.java) { listener(it) }

    fun addBackwardListener(listener: (ctrl: Boolean) -> Unit): Registration =
        addListener(BackwardEvent::class.java) { listener(it.ctrl) }

    fun addForwardListener(listener: (ctrl: Boolean) -> Unit): Registration =
        addListener(ForwardEvent::class.java) { listener(it.ctrl) }

    /**
     * Fired by the in-chart left/right edge overlays to page earlier / later in time. [ctrl] (Ctrl
     * held) pages a full interval instead of a quarter, matching the toolbar buttons.
     */
    @DomEvent("jvmguard-nav-backward")
    class BackwardEvent(
        source: EChart,
        fromClient: Boolean,
        @EventData("event.detail.ctrl") val ctrl: Boolean,
    ) : ComponentEvent<EChart>(source, fromClient)

    @DomEvent("jvmguard-nav-forward")
    class ForwardEvent(
        source: EChart,
        fromClient: Boolean,
        @EventData("event.detail.ctrl") val ctrl: Boolean,
    ) : ComponentEvent<EChart>(source, fromClient)

    /** Fired on a double click on the plot area (the time under the cursor). */
    @DomEvent("jvmguard-zoom-in")
    class ZoomInEvent(
        source: EChart,
        fromClient: Boolean,
        @EventData("event.detail.time") time: Double,
    ) : ComponentEvent<EChart>(source, fromClient) {
        val time: Long = time.toLong()
    }

    /** Fired on a click on a data point. */
    @DomEvent("jvmguard-point-click")
    class PointClickEvent(
        source: EChart,
        fromClient: Boolean,
        @EventData("event.detail.time") time: Double,
        @EventData("event.detail.seriesName") val seriesName: String,
    ) : ComponentEvent<EChart>(source, fromClient) {
        val time: Long = time.toLong()
    }

    /** Fired on right-click. Reports the time under the cursor so context-menu actions can use it. */
    @DomEvent("jvmguard-context-time")
    class ContextTimeEvent(
        source: EChart,
        fromClient: Boolean,
        @EventData("event.detail.time") time: Double,
    ) : ComponentEvent<EChart>(source, fromClient) {
        val time: Long = time.toLong()
    }

    /** Reports the rendered y-axis extent after each render. */
    @DomEvent("jvmguard-yaxis-extent")
    class YExtentEvent(
        source: EChart,
        fromClient: Boolean,
        @EventData("event.detail.min") val min: Double,
        @EventData("event.detail.max") val max: Double,
    ) : ComponentEvent<EChart>(source, fromClient)
}
