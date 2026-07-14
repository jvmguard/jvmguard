package dev.jvmguard.ui.components.recording.triggers

import dev.jvmguard.agent.config.transactions.ComparisonType
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.config.triggers.*
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.ThresholdIdentifier
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.components.recording.thresholds.thresholdDisplayName
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField

class TriggerDialog(
    private val trigger: Trigger,
    isNew: Boolean,
    private val thresholds: List<Threshold>,
    private val telemetryTypes: Collection<TelemetryType>,
    private val onSave: (Trigger) -> Unit,
) : JvmGuardDialog() {

    private val inhibitionTime = IntegerField("Inhibition time").apply { width = "8rem"; value = trigger.inhibitionTime }
    private val inhibitionInterval = EnumSelect("", Trigger.Interval::class.java) { it.multipleVerbose }
        .apply { value = trigger.inhibitionInterval }

    private val typeWriter: () -> Boolean

    init {
        headerTitle = (if (isNew) "Add " else "Edit ") + trigger.triggerType.toString().replaceFirstChar { it.lowercase() }
        width = "58rem"
        height = "46rem"

        val fields = VerticalLayout().apply { isPadding = false; isSpacing = true }
        typeWriter = buildType(fields)
        fields.add(labeled("Inhibition time", inhibitionTime, inhibitionInterval))

        val actionsEditor = TriggerActionsEditor(trigger.triggerActions)

        add(VerticalLayout(fields, actionsEditor).apply {
            setSizeFull()
            isPadding = false
            isSpacing = true
            setFlexGrow(1.0, actionsEditor)
        })

        confirmFooter("Save", ID_SAVE) { save() }
    }

    private fun buildType(fields: VerticalLayout): () -> Boolean = when (val t = trigger) {
        is ThresholdTrigger -> {
            val threshold = Select<Threshold>().apply {
                label = "Threshold"
                testId = ID_THRESHOLD
                setItems(thresholds)
                setItemLabelGenerator { thresholdDisplayName(it, telemetryTypes) }
                value = thresholds.firstOrNull { sameThreshold(it, t) }
            }
            val count = IntegerField("Fire after").apply { width = "8rem"; value = t.count }
            val interval = dataInterval(t)
            fields.add(threshold, fireAfterRow(count, interval))
            val writer: () -> Boolean = {
                if (threshold.value == null) {
                    threshold.isInvalid = true
                    threshold.errorMessage = "Select a threshold."
                    false
                } else {
                    t.thresholdIdentifier = ThresholdIdentifier(threshold.value.telemetryIdentifier, threshold.value.customName.usedValue)
                    t.count = count.value ?: 1
                    t.interval = interval.value
                    true
                }
            }
            writer
        }

        is PolicyTrigger -> {
            val filter = TextField("Filter").apply { setWidthFull(); value = t.filter }
            val comparison = EnumSelect("Comparison", ComparisonType::class.java) { it.toString() }.apply { value = t.comparisonType }
            val states = MultiSelectComboBox<PolicyState>("Transaction states").apply {
                setItems(PolicyState.entries)
                setItemLabelGenerator { it.label }
                setWidthFull()
                value = PolicyState.entries.filter { it.isSet(t) }.toSet()
            }
            val count = IntegerField("Fire after").apply { width = "8rem"; value = t.count }
            val interval = dataInterval(t)
            fields.add(filter, comparison, states, fireAfterRow(count, interval))
            val writer: () -> Boolean = {
                t.filter = filter.value
                t.comparisonType = comparison.value
                PolicyState.entries.forEach { it.set(t, it in states.value) }
                t.count = count.value ?: 1
                t.interval = interval.value
                true
            }
            writer
        }

        is ConnectionTrigger -> {
            val count = IntegerField("Minimum number of VMs").apply { width = "12rem"; value = t.count }
            val startMode = RadioButtonGroup<ConnectionTrigger.StartMode>().apply {
                label = "Trigger is armed"
                setItems(*ConnectionTrigger.StartMode.entries.toTypedArray())
                setItemLabelGenerator { it.toString() }
                value = t.startMode
            }
            val minTime = IntegerField("Minimum time").apply { width = "8rem"; value = t.minimumTime }
            val minUnit = EnumSelect("", TimeUnit::class.java) { it.toString() }.apply { value = t.minimumTimeUnit }
            fields.add(count, startMode, labeled("Minimum time", minTime, minUnit))
            val writer: () -> Boolean = {
                t.count = count.value ?: 1
                t.startMode = startMode.value
                t.minimumTime = minTime.value ?: 1
                t.minimumTimeUnit = minUnit.value
                true
            }
            writer
        }
    }

    private fun save() {
        if (!typeWriter()) {
            return
        }
        trigger.inhibitionTime = inhibitionTime.value ?: 0
        trigger.inhibitionInterval = inhibitionInterval.value
        onSave(trigger)
        close()
    }

    private fun dataInterval(t: DataTrigger): EnumSelect<Trigger.Interval> =
        EnumSelect("", Trigger.Interval::class.java) { it.toString() }.apply { value = t.interval }

    private fun labeled(label: String, value: IntegerField, unit: Component): HorizontalLayout {
        value.label = label
        return HorizontalLayout(value, unit).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            isPadding = false
        }
    }

    private fun fireAfterRow(count: IntegerField, interval: EnumSelect<Trigger.Interval>): HorizontalLayout {
        count.label = "Fire after"
        return HorizontalLayout(count, Span("events in one"), interval).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE
            isPadding = false
        }
    }

    private enum class PolicyState(
        val label: String,
        val isSet: (PolicyTrigger) -> Boolean,
        val set: (PolicyTrigger, Boolean) -> Unit,
    ) {
        NORMAL("Normal", { it.isNormal }, { t, v -> t.isNormal = v }),
        SLOW("Slow", { it.isSlow }, { t, v -> t.isSlow = v }),
        VERY_SLOW("Very slow", { it.isVerySlow }, { t, v -> t.isVerySlow = v }),
        OVERDUE("Overdue", { it.isOverdue }, { t, v -> t.isOverdue = v }),
        ERROR("Error", { it.isError }, { t, v -> t.isError = v }),
    }

    companion object {
        const val ID_THRESHOLD = "trigger-threshold"
        const val ID_SAVE = "trigger-save"

        private fun sameThreshold(threshold: Threshold, trigger: ThresholdTrigger): Boolean =
            trigger.thresholdIdentifier == ThresholdIdentifier(threshold.telemetryIdentifier, threshold.customName.usedValue)
    }
}
