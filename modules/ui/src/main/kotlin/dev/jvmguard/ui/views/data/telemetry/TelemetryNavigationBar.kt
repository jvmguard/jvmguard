package dev.jvmguard.ui.views.data.telemetry

import dev.jvmguard.data.vmdata.TelemetryInterval
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.select.Select
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * This is a controls holder, not a layout: it exposes controls so the view can lay them out in a
 * single shared toolbar. [tick] is called on each UI poll to drive auto-update.
 */
class TelemetryNavigationBar(
    private val serverTime: () -> Long,
    private val onChange: () -> Unit,
) {

    enum class EndMode(val label: String) { NOW("Now"), DATE("Selected date") }

    var selectedInterval: TelemetryInterval = TelemetryInterval.TEN_MINUTES
        private set

    private var endMode = EndMode.NOW
    private var noPreviousData = false
    private var pollTicks = 0
    private var animatedNav = false

    private val intervalSelect = Select<TelemetryInterval>().apply {
        label = "Show"
        setItems(*TelemetryInterval.entries.toTypedArray())
        value = selectedInterval
        testId = ID_INTERVAL
        addValueChangeListener { event ->
            if (event.isFromClient && event.value != null) {
                selectedInterval = event.value
                refresh()
            }
        }
    }

    private val endModeSelect = Select<EndMode>().apply {
        label = "Up to"
        setItems(*EndMode.entries.toTypedArray())
        setItemLabelGenerator { it.label }
        value = endMode
        testId = ID_END_MODE
        addValueChangeListener { event ->
            if (event.isFromClient && event.value != null) {
                if (event.value == EndMode.NOW) {
                    switchToNow()
                } else {
                    setDateMode(currentEndTime())
                }
                refresh()
            }
        }
    }

    private val dateField = DateTimePicker().apply {
        label = "End"
        step = Duration.ofMinutes(1)
        isVisible = false
        testId = ID_DATE
        addValueChangeListener { event ->
            if (event.isFromClient && event.value != null) {
                refresh()
            }
        }
    }

    // Ctrl-click pages by a full interval instead of a quarter (mirrors V1).
    private val previousButton = iconButton(VaadinIcon.ANGLE_LEFT, "Previous (Ctrl: full interval)", ID_PREVIOUS) { previous(it.isCtrlKey) }
    private val nextButton = iconButton(VaadinIcon.ANGLE_RIGHT, "Next (Ctrl: full interval)", ID_NEXT) { next(it.isCtrlKey) }
    private val zoomInButton = iconButton(VaadinIcon.SEARCH_PLUS, "Zoom in", ID_ZOOM_IN) { zoomIn(null) }
    private val zoomOutButton = iconButton(VaadinIcon.SEARCH_MINUS, "Zoom out", ID_ZOOM_OUT) { zoomOut(null) }

    private val autoUpdate = Checkbox("Auto-update").apply {
        testId = ID_AUTO_UPDATE
        addClassName("jvmguard-telemetry-autoupdate")
    }

    val navigationButtons: List<Component> = listOf(previousButton, nextButton, zoomInButton, zoomOutButton)
    val rangeControls: List<Component> = listOf(intervalSelect, endModeSelect, dateField, autoUpdate)

    init {
        updateButtonStates()
    }

    fun currentEndTime(): Long =
        if (endMode == EndMode.NOW) serverTime() else dateEndTime()

    fun isNowSelected(): Boolean = endMode == EndMode.NOW

    fun setPreviousEnabled(enabled: Boolean) {
        noPreviousData = !enabled
        previousButton.isEnabled = enabled
    }

    fun tick() {
        if (!autoUpdate.value || endMode != EndMode.NOW) {
            pollTicks = 0
            return
        }
        if (++pollTicks >= AUTO_UPDATE_TICKS) {
            pollTicks = 0
            onChange()
        }
    }

    fun zoomIn(center: Long?) {
        if (selectedInterval.ordinal == 0) {
            return
        }
        animatedNav = true
        selectedInterval = TelemetryInterval.entries[selectedInterval.ordinal - 1]
        intervalSelect.value = selectedInterval
        recenter(center)
        refresh()
    }

    fun zoomOut(center: Long?) {
        if (selectedInterval.ordinal == TelemetryInterval.entries.lastIndex) {
            return
        }
        animatedNav = true
        selectedInterval = TelemetryInterval.entries[selectedInterval.ordinal + 1]
        intervalSelect.value = selectedInterval
        recenter(center)
        refresh()
    }

    fun isZoomInEnabled(): Boolean = selectedInterval.ordinal > 0

    fun isZoomOutEnabled(): Boolean = selectedInterval.ordinal < TelemetryInterval.entries.lastIndex

    /**
     * Whether the change that triggered the current reload was a same-telemetry range navigation —
     * paging backward/forward or zooming in/out (consumed: resets to false on read). Only these
     * animate the y-axis rescale; everything else replaces instantly.
     */
    fun consumeAnimatedNav(): Boolean {
        val animated = animatedNav
        animatedNav = false
        return animated
    }

    /** Sets the interval and snaps to Now without firing a change. Used when opening from a sparkline. */
    fun applyRange(interval: TelemetryInterval) {
        selectedInterval = interval
        intervalSelect.value = interval
        switchToNow()
        updateButtonStates()
    }

    fun previous(page: Boolean = false) {
        animatedNav = true
        setDateMode(currentEndTime() - step(page))
        refresh()
    }

    fun next(page: Boolean = false) {
        animatedNav = true
        val newEnd = currentEndTime() + step(page)
        if (newEnd >= serverTime()) {
            switchToNow()
        } else {
            setDateMode(newEnd)
        }
        refresh()
    }

    private fun recenter(center: Long?) {
        if (center == null) {
            return
        }
        val newEnd = center + selectedInterval.timeExtent / 2
        if (newEnd >= serverTime()) {
            switchToNow()
        } else {
            setDateMode(newEnd)
        }
    }

    private fun step(page: Boolean): Long =
        if (page) selectedInterval.timeExtent else selectedInterval.timeExtent / PAGE_DIVISOR

    private fun switchToNow() {
        endMode = EndMode.NOW
        endModeSelect.value = EndMode.NOW
    }

    private fun setDateMode(endMillis: Long) {
        endMode = EndMode.DATE
        endModeSelect.value = EndMode.DATE
        dateField.value = LocalDateTime.ofInstant(Instant.ofEpochMilli(endMillis), ZoneId.systemDefault())
    }

    private fun dateEndTime(): Long =
        dateField.value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: serverTime()

    private fun refresh() {
        updateButtonStates()
        onChange()
    }

    private fun updateButtonStates() {
        dateField.isVisible = endMode == EndMode.DATE
        nextButton.isEnabled = endMode == EndMode.DATE
        previousButton.isEnabled = !noPreviousData
        zoomInButton.isEnabled = selectedInterval.ordinal > 0
        zoomOutButton.isEnabled = selectedInterval.ordinal < TelemetryInterval.entries.lastIndex
        autoUpdate.isVisible = endMode == EndMode.NOW
    }

    private fun iconButton(icon: VaadinIcon, label: String, id: String, onClick: (ClickEvent<Button>) -> Unit): Button =
        Button(icon.create()) { event -> onClick(event) }.apply {
            addThemeVariants(ButtonVariant.TERTIARY)
            setAriaLabel(label)
            setTooltipText(label)
            testId = id
        }

    companion object {
        const val ID_INTERVAL = "telemetry-interval"
        const val ID_END_MODE = "telemetry-end-mode"
        const val ID_DATE = "telemetry-end-date"
        const val ID_PREVIOUS = "telemetry-previous"
        const val ID_NEXT = "telemetry-next"
        const val ID_ZOOM_IN = "telemetry-zoom-in"
        const val ID_ZOOM_OUT = "telemetry-zoom-out"
        const val ID_AUTO_UPDATE = "telemetry-auto-update"

        private const val PAGE_DIVISOR = 4
        private const val AUTO_UPDATE_TICKS = 10
    }
}
