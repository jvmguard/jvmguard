package com.jvmguard.ui.views.data.transactions

import com.jvmguard.data.transactions.TransactionCursor
import com.jvmguard.data.transactions.TransactionTreeInterval
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

class TransactionNavigationBar(
    private val serverTime: () -> Long,
    private val onPrevious: () -> Unit,
    private val onNext: () -> Unit,
    private val onShowCurrent: () -> Unit,
    private val onShowTime: (Long) -> Unit,
    private val onIntervalChanged: () -> Unit,
    private val onAutoTick: () -> Unit,
) {

    enum class EndMode(val label: String) { NOW("Now"), DATE("Selected date") }

    var selectedInterval: TransactionTreeInterval = TransactionTreeInterval.TEN_MINUTE
        private set

    private var endMode = EndMode.NOW
    private var pollTicks = 0

    private val intervalSelect = Select<TransactionTreeInterval>().apply {
        label = "Show"
        setItems(*TransactionTreeInterval.entries.toTypedArray())
        value = selectedInterval
        testId = ID_INTERVAL
        addValueChangeListener { event ->
            if (event.isFromClient && event.value != null) {
                selectedInterval = event.value
                onIntervalChanged()
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
                    endMode = EndMode.NOW
                    updateControlVisibility()
                    onShowCurrent()
                } else {
                    endMode = EndMode.DATE
                    updateControlVisibility()
                    onShowTime(dateEndTime())
                }
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
                onShowTime(dateEndTime())
            }
        }
    }

    private val previousButton =
        iconButton(VaadinIcon.ANGLE_LEFT, "Previous interval", ID_PREVIOUS) { onPrevious() }
    private val nextButton =
        iconButton(VaadinIcon.ANGLE_RIGHT, "Next interval", ID_NEXT) { onNext() }

    private val autoUpdate = Checkbox("Auto-update").apply {
        testId = ID_AUTO_UPDATE
        addClassName("jvmguard-telemetry-autoupdate")
    }

    val navigationButtons: List<Component> = listOf(previousButton, nextButton)
    val rangeControls: List<Component> = listOf(intervalSelect, endModeSelect, dateField, autoUpdate)

    init {
        updateControlVisibility()
    }

    // set the interval without firing onIntervalChanged
    fun selectInterval(interval: TransactionTreeInterval) {
        selectedInterval = interval
        intervalSelect.value = interval
    }

    fun isAutoUpdate(): Boolean = autoUpdate.value && endMode == EndMode.NOW

    fun update(cursor: TransactionCursor) {
        if (selectedInterval.isAutoUpdateSupported && cursor.isLatest) {
            endMode = EndMode.NOW
            endModeSelect.value = EndMode.NOW
        } else {
            endMode = EndMode.DATE
            endModeSelect.value = EndMode.DATE
            val endTime = cursor.startTime + cursor.interval.timeExtent
            dateField.value = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault())
        }
        previousButton.isEnabled = cursor.isLatest || cursor.availability.isAvailable
        nextButton.isEnabled = !cursor.isLatest
        updateControlVisibility()
    }

    fun tick() {
        if (!isAutoUpdate()) {
            pollTicks = 0
            return
        }
        if (++pollTicks >= AUTO_UPDATE_TICKS) {
            pollTicks = 0
            onAutoTick()
        }
    }


    private fun dateEndTime(): Long =
        dateField.value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: serverTime()

    private fun updateControlVisibility() {
        dateField.isVisible = endMode == EndMode.DATE
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
        const val ID_INTERVAL = "transaction-interval"
        const val ID_END_MODE = "transaction-end-mode"
        const val ID_DATE = "transaction-end-date"
        const val ID_PREVIOUS = "transaction-previous"
        const val ID_NEXT = "transaction-next"
        const val ID_AUTO_UPDATE = "transaction-auto-update"

        private const val AUTO_UPDATE_TICKS = 10
    }
}
