package dev.jvmguard.ui.components.sparkline

import com.vaadin.flow.data.renderer.LitRenderer
import com.vaadin.flow.function.SerializableConsumer
import com.vaadin.flow.function.ValueProvider

object SparklineRenderers {

    // Mouse activation only; keyboard activation (Space on the focused cell) is handled at the grid
    // level (VmTreeGrid / TelemetryOverviewPanel), uniformly for the name and telemetry columns.
    private const val TEMPLATE =
        $$"<jvmguard-sparkline class=\"jvmguard-sparkline-link\" .state=${item.state} @click=${activate}></jvmguard-sparkline>"

    private const val DISPLAY_TEMPLATE =
        $$"<jvmguard-sparkline .state=${item.state}></jvmguard-sparkline>"

    fun <T> column(
        stateProvider: ValueProvider<T, SparklineState>, onActivate: SerializableConsumer<T>
    ): LitRenderer<T> =
        LitRenderer.of<T>(TEMPLATE)
            .withProperty("state", stateProvider)
            .withFunction("activate", onActivate)

    /** A non-interactive sparkline cell (no click), e.g. the overview's current-value column. */
    fun <T> display(stateProvider: ValueProvider<T, SparklineState>): LitRenderer<T> =
        LitRenderer.of<T>(DISPLAY_TEMPLATE)
            .withProperty("state", stateProvider)
}
