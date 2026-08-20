package dev.jvmguard.ui.components.recording.triggers

import dev.jvmguard.agent.config.transactions.ComparisonType
import dev.jvmguard.data.config.thresholds.Threshold
import dev.jvmguard.data.config.triggers.*
import dev.jvmguard.data.vmdata.TelemetryType
import dev.jvmguard.data.vmdata.ThresholdIdentifier
import dev.jvmguard.ui.components.EnumSelect
import dev.jvmguard.ui.components.JvmGuardDialog
import dev.jvmguard.ui.components.recording.thresholds.thresholdDisplayName
import dev.jvmguard.ui.server.enumLabel
import dev.jvmguard.ui.server.t
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

    private val inhibitionTime = IntegerField(t("trigger.inhibitionTime")).apply { width = "8rem"; value = trigger.inhibitionTime }
    private val inhibitionInterval = EnumSelect("", Trigger.Interval::class.java) { t("enum.Interval.plural.${it.name}") }
        .apply { value = trigger.inhibitionInterval }

    private val typeWriter: () -> Boolean

    init {
        headerTitle = t(
            if (isNew) "trigger.dialog.add" else "trigger.dialog.edit",
            enumLabel(trigger.triggerType).replaceFirstChar { it.lowercase() },
        )
        width = "58rem"
        height = "46rem"

        val fields = VerticalLayout().apply { isPadding = false; isSpacing = true }
        typeWriter = buildType(fields)
        fields.add(labeled(t("trigger.inhibitionTime"), inhibitionTime, inhibitionInterval))

        val actionsEditor = TriggerActionsEditor(trigger.triggerActions)

        add(VerticalLayout(fields, actionsEditor).apply {
            setSizeFull()
            isPadding = false
            isSpacing = true
            setFlexGrow(1.0, actionsEditor)
        })

        confirmFooter(t("common.save"), ID_SAVE) { save() }
    }

    private fun buildType(fields: VerticalLayout): () -> Boolean = when (val tr = trigger) {
        is ThresholdTrigger -> {
            val threshold = Select<Threshold>().apply {
                label = t("trigger.threshold")
                testId = ID_THRESHOLD
                setItems(thresholds)
                setItemLabelGenerator { thresholdDisplayName(it, telemetryTypes) }
                value = thresholds.firstOrNull { sameThreshold(it, tr) }
            }
            val count = IntegerField(t("trigger.fireAfter")).apply { width = "8rem"; value = tr.count }
            val interval = dataInterval(tr)
            fields.add(threshold, fireAfterRow(count, interval))
            val writer: () -> Boolean = {
                if (threshold.value == null) {
                    threshold.isInvalid = true
                    threshold.errorMessage = t("trigger.threshold.required")
                    false
                } else {
                    tr.thresholdIdentifier = ThresholdIdentifier(threshold.value.telemetryIdentifier, threshold.value.customName.usedValue)
                    tr.count = count.value ?: 1
                    tr.interval = interval.value
                    true
                }
            }
            writer
        }

        is PolicyTrigger -> {
            val filter = TextField(t("recording.tab.filter")).apply { setWidthFull(); value = tr.filter }
            val comparison = EnumSelect(t("recording.comparison"), ComparisonType::class.java).apply { value = tr.comparisonType }
            val states = MultiSelectComboBox<PolicyState>(t("trigger.policyStates")).apply {
                setItems(PolicyState.entries)
                setItemLabelGenerator { t(it.labelKey) }
                setWidthFull()
                value = PolicyState.entries.filter { it.isSet(tr) }.toSet()
            }
            val count = IntegerField(t("trigger.fireAfter")).apply { width = "8rem"; value = tr.count }
            val interval = dataInterval(tr)
            fields.add(filter, comparison, states, fireAfterRow(count, interval))
            val writer: () -> Boolean = {
                tr.filter = filter.value
                tr.comparisonType = comparison.value
                PolicyState.entries.forEach { it.set(tr, it in states.value) }
                tr.count = count.value ?: 1
                tr.interval = interval.value
                true
            }
            writer
        }

        is ConnectionTrigger -> {
            val count = IntegerField(t("trigger.minVms")).apply { width = "12rem"; value = tr.count }
            val startMode = RadioButtonGroup<ConnectionTrigger.StartMode>().apply {
                label = t("trigger.armed")
                setItems(*ConnectionTrigger.StartMode.entries.toTypedArray())
                setItemLabelGenerator { enumLabel(it) }
                value = tr.startMode
            }
            val minTime = IntegerField(t("recording.minimumTime")).apply { width = "8rem"; value = tr.minimumTime }
            val minUnit = EnumSelect("", TimeUnit::class.java) { t("enum.TimeUnit.plural.${it.name}") }.apply { value = tr.minimumTimeUnit }
            fields.add(count, startMode, labeled(t("recording.minimumTime"), minTime, minUnit))
            val writer: () -> Boolean = {
                tr.count = count.value ?: 1
                tr.startMode = startMode.value
                tr.minimumTime = minTime.value ?: 1
                tr.minimumTimeUnit = minUnit.value
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
        EnumSelect("", Trigger.Interval::class.java).apply { value = t.interval }

    private fun labeled(label: String, value: IntegerField, unit: Component): HorizontalLayout {
        value.label = label
        return HorizontalLayout(value, unit).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.END
            isPadding = false
        }
    }

    private fun fireAfterRow(count: IntegerField, interval: EnumSelect<Trigger.Interval>): HorizontalLayout {
        count.label = t("trigger.fireAfter")
        return HorizontalLayout(count, Span(t("trigger.fireAfter.eventsInOne")), interval).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE
            isPadding = false
        }
    }

    private enum class PolicyState(
        val labelKey: String,
        val isSet: (PolicyTrigger) -> Boolean,
        val set: (PolicyTrigger, Boolean) -> Unit,
    ) {
        NORMAL("trigger.policyState.normal", { it.isNormal }, { t, v -> t.isNormal = v }),
        SLOW("recording.policy.slow", { it.isSlow }, { t, v -> t.isSlow = v }),
        VERY_SLOW("recording.policy.verySlow", { it.isVerySlow }, { t, v -> t.isVerySlow = v }),
        OVERDUE("recording.policy.overdue", { it.isOverdue }, { t, v -> t.isOverdue = v }),
        ERROR("trigger.policyState.error", { it.isError }, { t, v -> t.isError = v }),
    }

    companion object {
        const val ID_THRESHOLD = "trigger-threshold"
        const val ID_SAVE = "trigger-save"

        private fun sameThreshold(threshold: Threshold, trigger: ThresholdTrigger): Boolean =
            trigger.thresholdIdentifier == ThresholdIdentifier(threshold.telemetryIdentifier, threshold.customName.usedValue)
    }
}
